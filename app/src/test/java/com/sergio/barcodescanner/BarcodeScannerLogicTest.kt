package com.sergio.barcodescanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeItemTest {

    @Test
    fun `barcode item creation with default values`() {
        val item = BarcodeItem(code = "123456")

        assertEquals("123456", item.code)
        assertEquals(null, item.imagePath)
        assertFalse(item.isSelected)
        assertFalse(item.id.isEmpty())
    }

    @Test
    fun `barcode item creation with all values`() {
        val item = BarcodeItem(
            id = "custom-id",
            code = "789012",
            imagePath = "/path/to/image.jpg",
            isSelected = true
        )

        assertEquals("custom-id", item.id)
        assertEquals("789012", item.code)
        assertEquals("/path/to/image.jpg", item.imagePath)
        assertTrue(item.isSelected)
    }

    @Test
    fun `barcode item copy with modified selection`() {
        val original = BarcodeItem(code = "123456", isSelected = false)
        val copied = original.copy(isSelected = true)

        assertEquals("123456", copied.code)
        assertTrue(copied.isSelected)
        assertEquals(original.id, copied.id)
    }

    @Test
    fun `barcode items with same code are equal`() {
        val item1 = BarcodeItem(code = "123456")
        val item2 = BarcodeItem(code = "123456")

        assertEquals(item1, item2)
    }

    @Test
    fun `barcode items with different code are not equal`() {
        val item1 = BarcodeItem(code = "123456")
        val item2 = BarcodeItem(code = "789012")

        assertFalse(item1 == item2)
    }
}

class SelectAllStateTest {

    @Test
    fun `select all state transitions`() {
        val items = mutableListOf<BarcodeItem>(
            BarcodeItem(code = "1", isSelected = false),
            BarcodeItem(code = "2", isSelected = false),
            BarcodeItem(code = "3", isSelected = false)
        )

        val state = when {
            items.isEmpty() -> SelectAllState.Unchecked
            items.all { it.isSelected } -> SelectAllState.Checked
            items.any { it.isSelected } -> SelectAllState.Indeterminate
            else -> SelectAllState.Unchecked
        }

        assertEquals(SelectAllState.Unchecked, state)
    }

    @Test
    fun `select all state when all selected`() {
        val items = mutableListOf<BarcodeItem>(
            BarcodeItem(code = "1", isSelected = true),
            BarcodeItem(code = "2", isSelected = true)
        )

        val state = when {
            items.isEmpty() -> SelectAllState.Unchecked
            items.all { it.isSelected } -> SelectAllState.Checked
            items.any { it.isSelected } -> SelectAllState.Indeterminate
            else -> SelectAllState.Unchecked
        }

        assertEquals(SelectAllState.Checked, state)
    }

    @Test
    fun `select all state when partially selected`() {
        val items = mutableListOf<BarcodeItem>(
            BarcodeItem(code = "1", isSelected = true),
            BarcodeItem(code = "2", isSelected = false)
        )

        val state = when {
            items.isEmpty() -> SelectAllState.Unchecked
            items.all { it.isSelected } -> SelectAllState.Checked
            items.any { it.isSelected } -> SelectAllState.Indeterminate
            else -> SelectAllState.Unchecked
        }

        assertEquals(SelectAllState.Indeterminate, state)
    }
}

class BarcodeSelectionLogicTest {

    @Test
    fun `select all click toggles all items to selected`() {
        val items = mutableListOf(
            BarcodeItem(code = "1", isSelected = false),
            BarcodeItem(code = "2", isSelected = false),
            BarcodeItem(code = "3", isSelected = false)
        )

        val isAllSelected = items.isNotEmpty() && items.all { it.isSelected }
        val newValue = !isAllSelected
        items.forEach { it.isSelected = newValue }

        assertTrue(items.all { it.isSelected })
    }

    @Test
    fun `select all click toggles all items to unselected`() {
        val items = mutableListOf(
            BarcodeItem(code = "1", isSelected = true),
            BarcodeItem(code = "2", isSelected = true),
            BarcodeItem(code = "3", isSelected = true)
        )

        val isAllSelected = items.isNotEmpty() && items.all { it.isSelected }
        val newValue = !isAllSelected
        items.forEach { it.isSelected = newValue }

        assertTrue(items.none { it.isSelected })
    }

    @Test
    fun `delete removes only selected items`() {
        val items = mutableListOf(
            BarcodeItem(code = "1", isSelected = true, imagePath = "/path/1.jpg"),
            BarcodeItem(code = "2", isSelected = false),
            BarcodeItem(code = "3", isSelected = true, imagePath = "/path/3.jpg")
        )

        val toRemove = items.filter { it.isSelected }.toList()
        items.removeAll(toRemove)

        assertEquals(1, items.size)
        assertEquals("2", items.first().code)
        assertFalse(items.first().isSelected)
    }

    @Test
    fun `delete clears selection after removal`() {
        val items = mutableListOf(
            BarcodeItem(code = "1", isSelected = true),
            BarcodeItem(code = "2", isSelected = true)
        )

        val toRemove = items.filter { it.isSelected }.toList()
        items.removeAll(toRemove)
        items.forEach { it.isSelected = false }

        assertTrue(items.isEmpty())
    }

    @Test
    fun `duplicate barcode detection`() {
        val items = mutableListOf(
            BarcodeItem(code = "123456"),
            BarcodeItem(code = "789012")
        )

        val barcodeValue = "123456"
        val exists = items.any { it.code == barcodeValue }

        assertTrue(exists)
    }

    @Test
    fun `non-duplicate barcode detection`() {
        val items = mutableListOf(
            BarcodeItem(code = "123456"),
            BarcodeItem(code = "789012")
        )

        val barcodeValue = "345678"
        val exists = items.any { it.code == barcodeValue }

        assertFalse(exists)
    }

    @Test
    fun `has selection returns true when any item selected`() {
        val items = mutableListOf(
            BarcodeItem(code = "1", isSelected = false),
            BarcodeItem(code = "2", isSelected = true)
        )

        val hasSelection = items.any { it.isSelected }

        assertTrue(hasSelection)
    }

    @Test
    fun `has selection returns false when no items selected`() {
        val items = mutableListOf(
            BarcodeItem(code = "1", isSelected = false),
            BarcodeItem(code = "2", isSelected = false)
        )

        val hasSelection = items.any { it.isSelected }

        assertFalse(hasSelection)
    }

    @Test
    fun `image path is preserved in barcode item`() {
        val imagePath = "/external/files/Pictures/1234567890.jpg"
        val item = BarcodeItem(code = "123456", imagePath = imagePath)

        assertEquals(imagePath, item.imagePath)
    }

    @Test
    fun `barcode item with null image path`() {
        val item = BarcodeItem(code = "123456", imagePath = null)

        assertEquals(null, item.imagePath)
    }
}
