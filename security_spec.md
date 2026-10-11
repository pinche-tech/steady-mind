# SteadyMind Security Specification (Phase 0 TDD)

## 1. Data Invariants
1. **Multi-User Isolation**: Every document in `/users/{userId}/tasks/{taskId}`, `/users/{userId}/thought_logs/{logId}`, and `/users/{userId}/focus_sessions/{sessionId}` belongs exclusively to `userId` and can only be read, created, updated, or deleted when `request.auth != null && request.auth.uid == userId` and `data.userId == request.auth.uid`.
2. **Strict Schema & Bounds**: Every string field enforces a non-empty minimum and explicit maximum `.size()`. Every integer field enforces numeric range bounds (`dreadLevel` 1..10, `anxietyBefore`/`anxietyAfter` 0..100, `durationMinutes` 1..180, `reliefRating` 1..10).
3. **Bounded Lists**: `microSteps` in `tasks` must be a `list` with `.size() <= 20` and valid string element check.
4. **Temporal Integrity & Immutability**: `createdAt` and `updatedAt` must be Firestore `timestamp` values `<= request.time`. On updates, `userId` and `createdAt` are strictly immutable.
5. **Terminal State Locking**: Once a task's `status` reaches `'completed'`, no further updates are permitted (`existing().status != 'completed'`). Focus sessions are append-only (`allow update: if false`).
6. **Secure List Queries**: Every `allow list` rule enforces `request.auth.uid == userId && resource.data.userId == request.auth.uid`.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read/Write**: `auth = null` attempting `get`, `list`, or `create` on `/users/alice_123/tasks`.
2. **Cross-User Read/Write**: `auth.uid = "bob_456"` attempting to read or mutate `/users/alice_123/tasks/task_1`.
3. **Identity Spoofing on Create**: `auth.uid = "alice_123"` creating `/users/alice_123/tasks/task_1` with `{"userId": "bob_456"}`.
4. **Shadow Field Injection (Ghost Key)**: Creating or updating a task with extra field `{"isAdmin": true}` or `{"isVerified": true}`.
5. **Immutable Field Tampering**: Updating a task or thought log by changing `createdAt` or `userId`.
6. **Terminal State Violation**: Updating a task whose `existing().status == "completed"` back to `"active"` or changing its title.
7. **String Size Overflow (Denial of Wallet)**: Creating a task with a 500-character `title` (limit 200) or invalid `blockerType`.
8. **Out-of-Bounds Integer**: Creating a task with `dreadLevel: 99` (limit 1..10) or thought log with `anxietyBefore: 150` (limit 0..100).
9. **Unbounded Array Injection**: Creating a task with 25 `microSteps` (limit 20).
10. **Future Timestamp Spoofing**: Creating a document with `createdAt` set in the future (`> request.time`).
11. **Unfiltered List Query**: Querying `/users/alice_123/tasks` without `.where("userId", "==", "alice_123")`.
12. **Immutable Focus Session Mutation**: Attempting `update` on `/users/alice_123/focus_sessions/session_1`.
