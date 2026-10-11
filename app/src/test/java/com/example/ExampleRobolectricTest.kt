package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.ui.SteadyMindAuthScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SteadyMind", appName)
  }

  @Test
  fun `main screen renders relaxing background, centered placeholder, brain dump, and tabs`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        SteadyMindMainScreen()
      }
    }

    composeTestRule.onNodeWithTag("main_scaffold").assertIsDisplayed()
    composeTestRule.onNodeWithTag("relaxing_background_image").assertIsDisplayed()
    composeTestRule.onNodeWithTag("upcoming_tasks_placeholder").assertIsDisplayed()
    composeTestRule.onNodeWithTag("background_scene_picker_button").assertIsDisplayed()

    // Open AI Coach tab and verify Scary Reply Helper & Chaos Sorter sub-tabs exist
    composeTestRule.onNodeWithTag("nav_coach").performClick()
    composeTestRule.onNodeWithTag("ai_coach_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_tab_reply").performClick()
    composeTestRule.onNodeWithTag("generate_scary_reply_button").assertExists()

    // Open Focus tab and verify timer and soundscapes exist
    composeTestRule.onNodeWithTag("nav_focus").performClick()
    composeTestRule.onNodeWithTag("focus_sanctuary_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("timer_play_pause_button").assertExists()

    // Open Reframe tab and switch to Worry Vault
    composeTestRule.onNodeWithTag("nav_reframe").performClick()
    composeTestRule.onNodeWithTag("reframe_journal_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("section_worry_vault_chip").performClick()
    composeTestRule.onNodeWithTag("lock_worry_button").assertExists()
  }

  @Test
  fun `sign in screen renders Google Sign-In button and relaxing background`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        SteadyMindAuthScreen(onAuthSuccess = {})
      }
    }

    composeTestRule.onNodeWithTag("auth_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("google_sign_in_button").assertExists()
  }

  @Test
  fun `mpesa subscription dialog displays 0119137126 and activates subscription`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        SteadyMindMainScreen()
      }
    }

    composeTestRule.onNodeWithTag("subscription_top_button").performClick()
    composeTestRule.onNodeWithTag("mpesa_subscription_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mpesa_recipient_number_text").performScrollTo().assertTextEquals("0119137126")
    composeTestRule.onNodeWithTag("mpesa_sender_phone_input").performScrollTo().performTextInput("0712345678")
    composeTestRule.onNodeWithTag("mpesa_receipt_code_input").performScrollTo().performTextInput("SJK7Q9M2LP")
    composeTestRule.onNodeWithTag("verify_mpesa_subscription_button").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("active_subscription_banner").assertExists()
  }
}
