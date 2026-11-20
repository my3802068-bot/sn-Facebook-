package com.example

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.components.FacebookPaneView
import com.example.ui.components.MultiAccountManager
import com.example.ui.components.NushaTopBar
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureWebViewCacheDirs()
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                NushaLiteApp()
            }
        }
    }

    private fun ensureWebViewCacheDirs() {
        try {
            val baseCache = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            java.io.File(baseCache, "js").mkdirs()
            java.io.File(baseCache, "wasm").mkdirs()
        } catch (_: Throwable) {
        }
    }
}

@Composable
fun NushaLiteApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // File chooser for photo/file uploads inside Facebook WebViews
    var fileUploadCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intentData = result.data
            val uris: Array<Uri>? = when {
                intentData?.data != null -> arrayOf(intentData.data!!)
                intentData?.clipData != null -> {
                    val count = intentData.clipData!!.itemCount
                    Array(count) { i -> intentData.clipData!!.getItemAt(i).uri }
                }
                else -> null
            }
            fileUploadCallback?.onReceiveValue(uris)
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    val manager = remember {
        MultiAccountManager(context) { callback, params ->
            fileUploadCallback?.onReceiveValue(null)
            fileUploadCallback = callback
            try {
                val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                fileChooserLauncher.launch(intent)
                true
            } catch (_: Exception) {
                fileUploadCallback = null
                false
            }
        }
    }

    // Preserve active WebViews across 1 / 2 / 3 / 4 layout changes
    val paneContents = remember(manager) {
        List(4) { index ->
            movableContentOf<Int, Boolean> { totalPanes, compactWidth ->
                FacebookPaneView(
                    paneIndex = index,
                    totalPanes = totalPanes,
                    isCompactWidth = compactWidth,
                    isMultiSyncEnabled = manager.isMultiSyncEnabled,
                    isLastActive = manager.lastActivePane == index,
                    manager = manager,
                    onPaneTouched = { touchedIdx ->
                        manager.lastActivePane = touchedIdx
                    }
                )
            }
        }
    }

    BackHandler(enabled = manager.canAnyGoBack()) {
        manager.handleSystemBack()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0B0D))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0A0B0D)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NushaTopBar(
                accountCount = manager.accountCount,
                isMultiSyncEnabled = manager.isMultiSyncEnabled,
                onSelectAccountCount = { count ->
                    manager.updateAccountCount(count)
                    coroutineScope.launch {
                        val msg = if (count == 1) {
                            "Single Facebook page active"
                        } else {
                            "$count Facebook Login pages opened • Multi Sync ON"
                        }
                        snackbarHostState.showSnackbar(msg)
                    }
                },
                onSetMultiSync = { enabled ->
                    manager.setMultiSync(enabled)
                    coroutineScope.launch {
                        val msg = if (enabled) {
                            "Multi Sync ON: Actions in 1 page mirror to all"
                        } else {
                            "Manual Mode (OFF): Tap each page individually"
                        }
                        snackbarHostState.showSnackbar(msg)
                    }
                },
                onReloadAll = {
                    manager.reloadAll()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Reloading Facebook...")
                    }
                },
                onClearAllData = {
                    manager.clearAllData {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("All Facebook data & cookies cleared!")
                        }
                    }
                }
            )

            // Multi-Account Home Page Grid (1 to 4 Facebook panes)
            val count = manager.accountCount
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0A0B0D))
                    .then(if (count > 1) Modifier.padding(4.dp) else Modifier)
            ) {
                when (count) {
                    1 -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            paneContents[0](1, false)
                        }
                    }
                    2 -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                paneContents[0](2, false)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                paneContents[1](2, false)
                            }
                        }
                    }
                    3 -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    paneContents[0](3, true)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    paneContents[1](3, true)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                paneContents[2](3, false)
                            }
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    paneContents[0](4, true)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    paneContents[1](4, true)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    paneContents[2](4, true)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    paneContents[3](4, true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
