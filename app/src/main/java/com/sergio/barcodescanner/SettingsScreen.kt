package com.sergio.barcodescanner

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sergio.barcodescanner.ui.theme.ThemeMode
import com.sergio.barcodescanner.ui.theme.ThemePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentThemeMode: ThemeMode,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var themeMode by remember { mutableStateOf(currentThemeMode) }
    var compareBySuffix by rememberSaveable { mutableStateOf(ThemePreference.isCompareBySuffix(context)) }
    var suffixLength by rememberSaveable { mutableIntStateOf(ThemePreference.getCompareSuffixLength(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Тема приложения", style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RadioButton(
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = {
                        themeMode = ThemeMode.LIGHT
                        ThemePreference.setThemeMode(context, ThemeMode.LIGHT)
                        (context as ComponentActivity).recreate()
                    }
                )
                Text("Светлая")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RadioButton(
                    selected = themeMode == ThemeMode.DARK,
                    onClick = {
                        themeMode = ThemeMode.DARK
                        ThemePreference.setThemeMode(context, ThemeMode.DARK)
                        (context as ComponentActivity).recreate()
                    }
                )
                Text("Темная")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RadioButton(
                    selected = themeMode == ThemeMode.AUTO,
                    onClick = {
                        themeMode = ThemeMode.AUTO
                        ThemePreference.setThemeMode(context, ThemeMode.AUTO)
                        (context as ComponentActivity).recreate()
                    }
                )
                Text("Авто")
            }

            Spacer(modifier = Modifier.padding(vertical = 16.dp))

            Text("Сравнение штрихкодов", style = MaterialTheme.typography.titleMedium)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.Checkbox(
                    checked = compareBySuffix,
                    onCheckedChange = { checked ->
                        compareBySuffix = checked
                        ThemePreference.setCompareBySuffix(context, checked)
                    }
                )
                Text("Сравнивать по последним символам")
            }

            if (compareBySuffix) {
                Text(
                    text = "Количество символов для сравнения: ${suffixLength.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                )
                Slider(
                    value = suffixLength.toFloat(),
                    onValueChange = { value ->
                        suffixLength = value.toInt()
                        ThemePreference.setCompareSuffixLength(context, value.toInt())
                    },
                    valueRange = 1f..20f,
                    steps = 18,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
