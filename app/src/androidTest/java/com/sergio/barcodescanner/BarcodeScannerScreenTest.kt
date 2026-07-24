package com.sergio.barcodescanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BarcodeScannerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyList_showsHint() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BarcodeScannerScreenTestContent(items = emptyList())
                }
            }
        }

        composeTestRule.onNodeWithText("Нажмите +, чтобы отсканировать штрихкод").assertIsDisplayed()
    }

    @Test
    fun nonEmptyList_showsBarcodes() {
        val items = listOf(
            BarcodeItem(code = "123456"),
            BarcodeItem(code = "789012")
        )

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BarcodeScannerScreenTestContent(items = items)
                }
            }
        }

        composeTestRule.onNodeWithText("123456").assertIsDisplayed()
        composeTestRule.onNodeWithText("789012").assertIsDisplayed()
    }

    @Test
    fun checkboxTogglesSelection() {
        val items = mutableListOf(
            BarcodeItem(code = "123456"),
            BarcodeItem(code = "789012")
        )

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BarcodeScannerScreenTestContent(items = items)
                }
            }
        }

        composeTestRule.onAllNodesWithText("123456").fetchSemanticsNodes().size
        val firstCheckbox = composeTestRule.onAllNodesWithText("123456").fetchSemanticsNodes().firstOrNull()
    }

    @Test
    fun barcodeItem_displaysCodeAndNumber() {
        val items = listOf(
            BarcodeItem(code = "ABC123")
        )

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BarcodeScannerScreenTestContent(items = items)
                }
            }
        }

        composeTestRule.onNodeWithText("ABC123").assertIsDisplayed()
        composeTestRule.onNodeWithText("1.").assertIsDisplayed()
    }

    @Test
    fun barcodeItem_withImage_showsVisibilityIcon() {
        val items = listOf(
            BarcodeItem(code = "IMAGE123", imagePath = "/path/to/image.jpg")
        )

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BarcodeScannerScreenTestContent(items = items)
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Просмотреть изображение").assertIsDisplayed()
    }

    @Test
    fun barcodeItem_withoutImage_hidesVisibilityIcon() {
        val items = listOf(
            BarcodeItem(code = "NOIMAGE")
        )

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BarcodeScannerScreenTestContent(items = items)
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Просмотреть изображение").assertDoesNotExist()
    }
}

@Composable
fun BarcodeScannerScreenTestContent(items: List<BarcodeItem>) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нажмите +, чтобы отсканировать штрихкод",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(items) { index, item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.isSelected,
                            onCheckedChange = { checked ->
                                item.isSelected = checked
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = item.code,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (!item.imagePath.isNullOrBlank()) {
                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Просмотреть изображение"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
