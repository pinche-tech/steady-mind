package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.MPESA_RECIPIENT_DISPLAY
import com.example.data.MPESA_RECIPIENT_NUMBER
import com.example.data.MpesaSubscription
import com.example.data.SubscriptionPlan

@Composable
fun MpesaSubscriptionDialog(
  subscriptions: List<MpesaSubscription>,
  onActivateSubscription: (
    plan: SubscriptionPlan,
    senderPhone: String,
    mpesaReceiptCode: String,
    onResult: (Boolean, String) -> Unit
  ) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val activeSubscription = subscriptions.firstOrNull { it.isActive }

  var selectedPlan by rememberSaveable { mutableStateOf(SubscriptionPlan.MONTHLY) }
  var senderPhone by rememberSaveable { mutableStateOf("") }
  var mpesaReceiptCode by rememberSaveable { mutableStateOf("") }
  var copiedNotice by remember { mutableStateOf(false) }
  var feedbackMessage by remember { mutableStateOf<String?>(null) }
  var isErrorFeedback by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("mpesa_subscription_dialog"),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.WorkspacePremium,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "SteadyMind Plus • M-Pesa",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Support your calm focus sanctuary via M-Pesa",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Active Subscription Banner if already subscribed
        if (activeSubscription != null) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("active_subscription_banner")
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Verified,
                  contentDescription = "Verified Subscription",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "${activeSubscription.planTitle} Active",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
              Text(
                text = "Paid KES ${activeSubscription.amountKes} to M-Pesa ${activeSubscription.recipientMpesaNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = "Receipt #${activeSubscription.mpesaReceiptCode} • Phone: ${activeSubscription.senderPhone}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
              )
            }
          }
        }

        // Step 1: Choose Subscription Plan
        Text(
          text = "1. Choose Your SteadyMind Plus Plan",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )

        SubscriptionPlan.entries.forEach { plan ->
          val isSelected = selectedPlan == plan
          Surface(
            color = if (isSelected) {
              MaterialTheme.colorScheme.secondaryContainer
            } else {
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
            shape = RoundedCornerShape(16.dp),
            border = if (isSelected) {
              BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
              null
            },
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedPlan = plan }
              .testTag("plan_card_${plan.id}")
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  if (plan.badge != null) {
                    Surface(
                      color = MaterialTheme.colorScheme.primary,
                      shape = RoundedCornerShape(50)
                    ) {
                      Text(
                        text = plan.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                      )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                  }
                  Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                  )
                }

                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = "KES ${plan.priceKes}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Text(
                    text = plan.billingCycle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              plan.perks.forEach { perk ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                  )
                  Text(
                    text = perk,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }

        // Step 2: M-Pesa Payment Details Card (0119137126)
        Text(
          text = "2. Send M-Pesa Payment",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )

        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("mpesa_payment_instructions_card")
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "Go to M-Pesa -> Send Money and send KES ${selectedPlan.priceKes} to:",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(
                    text = "M-Pesa Number",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = MPESA_RECIPIENT_NUMBER,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("mpesa_recipient_number_text")
                  )
                  Text(
                    text = "($MPESA_RECIPIENT_DISPLAY • Amount: KES ${selectedPlan.priceKes})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                OutlinedButton(
                  onClick = {
                    copyMpesaNumberToClipboard(context, MPESA_RECIPIENT_NUMBER)
                    copiedNotice = true
                  },
                  shape = RoundedCornerShape(50),
                  modifier = Modifier.testTag("copy_mpesa_number_button")
                ) {
                  Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy M-Pesa number",
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(if (copiedNotice) "Copied!" else "Copy")
                }
              }
            }

            OutlinedButton(
              onClick = {
                openMpesaDialer(context)
              },
              shape = RoundedCornerShape(50),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("dial_mpesa_ussd_button")
            ) {
              Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Dial *334# to Open M-Pesa Menu")
            }
          }
        }

        // Step 3: Enter M-Pesa Confirmation Code to Activate
        Text(
          text = "3. Verify Your M-Pesa Transaction",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
          value = senderPhone,
          onValueChange = {
            senderPhone = it
            feedbackMessage = null
          },
          label = { Text("Your M-Pesa Phone Number") },
          placeholder = { Text("e.g. 0712345678 or 0119137126") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("mpesa_sender_phone_input")
        )

        OutlinedTextField(
          value = mpesaReceiptCode,
          onValueChange = {
            mpesaReceiptCode = it.uppercase()
            feedbackMessage = null
          },
          label = { Text("M-Pesa SMS Transaction Code") },
          placeholder = { Text("e.g. SJK7Q9M2LP") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("mpesa_receipt_code_input")
        )

        val currentFeedback = feedbackMessage
        if (currentFeedback != null) {
          Surface(
            color = if (isErrorFeedback) {
              MaterialTheme.colorScheme.errorContainer
            } else {
              MaterialTheme.colorScheme.primaryContainer
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("mpesa_verification_feedback")
          ) {
            Text(
              text = currentFeedback,
              style = MaterialTheme.typography.bodySmall,
              color = if (isErrorFeedback) {
                MaterialTheme.colorScheme.onErrorContainer
              } else {
                MaterialTheme.colorScheme.onPrimaryContainer
              },
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        Button(
          onClick = {
            onActivateSubscription(selectedPlan, senderPhone, mpesaReceiptCode) { success, msg ->
              isErrorFeedback = !success
              feedbackMessage = msg
              if (success) {
                mpesaReceiptCode = ""
              }
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          shape = RoundedCornerShape(50),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("verify_mpesa_subscription_button")
        ) {
          Icon(
            imageVector = Icons.Default.Verified,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Verify M-Pesa & Activate (${selectedPlan.title})")
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("close_subscription_dialog_button")
      ) {
        Text("Done")
      }
    }
  )
}

private fun copyMpesaNumberToClipboard(context: Context, number: String) {
  runCatching {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText("SteadyMind M-Pesa Number", number)
    clipboard?.setPrimaryClip(clip)
  }
}

private fun openMpesaDialer(context: Context) {
  runCatching {
    val intent = Intent(Intent.ACTION_DIAL).apply {
      data = Uri.parse("tel:${Uri.encode("*334#")}")
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
  }
}
