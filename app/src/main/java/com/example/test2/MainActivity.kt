package com.example.test2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.test2.ui.theme.Test2Theme
import com.google.ar.core.Config
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.node.PlacementMode
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.text.style.TextAlign
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.PI
import com.google.zxing.integration.android.IntentIntegrator
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.tooling.preview.Preview

object QRTextHolder {
    val scannedQRText: MutableState<String> = mutableStateOf("No QR Code Scanned")
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Test2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val activity = this@MainActivity // Get reference to the activity



                    Box (modifier = Modifier.fillMaxSize()) {
                        val currentModel = remember {
                            mutableStateOf("chair")
                        }
                        ARScreen(QRTextHolder.scannedQRText, activity)
                        Furniture(modifier=Modifier.align(Alignment.BottomCenter)){
                            currentModel.value = it
                        }

                        // Buttons to be conditionally displayed based on 'uiVisible'

                            IconButton(onClick = {startQRScanner()}, modifier = Modifier.align(TopEnd)) {
                                Icon(
                                    painter = painterResource(id = R.drawable.baseline_qr_code_scanner_24),
                                    contentDescription = "QR Code Scanner",
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                    }
                }
            }
        }
    }

    private fun startQRScanner() {
        val integrator = IntentIntegrator(this)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IntentIntegrator.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result != null && result.contents != null) {
                val url = result.contents
                val urlParts = url.split("/")
                val textFromUrl = urlParts.lastOrNull()?.replace(".glb", "") ?: "No QR Code Scanned"
                QRTextHolder.scannedQRText.value = textFromUrl
            }
        }
    }
}

//The furniture itself
@Composable
fun Furniture(modifier: Modifier, onClick: (String) -> Unit) {
    var currentIndex by remember {
        mutableStateOf(0)
    }
    val itemsList = listOf(
        Chair("White Chair", R.drawable.chairicon)
    )

    fun updateIndex(offset: Int) {
        currentIndex = (currentIndex + itemsList.size + offset) % itemsList.size
        onClick(itemsList[currentIndex].name)
    }
}
//Close the furniture

//Checks placement indication
@Composable
fun ObjectPlacementIndicator(isReady: Boolean) {
    val color = if (isReady) Color.Green else Color.Red

    Box(
        modifier = Modifier
            .padding(16.dp)
            .size(100.dp)
            .background(color = color)
    ) {
        Text(
            text = if (isReady) "Place Object" else "Finding Surface...",
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
// Close Checks placement indication


@Composable
fun ARScreen(scannedQRText: MutableState<String>, activity: ComponentActivity) {
    val nodes = remember { mutableStateListOf<ArNode>() }
    val modelNode = remember { mutableStateOf<ArModelNode?>(null) }
    val placeModelButton = remember { mutableStateOf(false) }
    var objectPlaced by remember { mutableStateOf(false) }

    var horizontalRotationCount by remember {
        mutableStateOf(0)
    }
    var objectPlacementReady by remember { mutableStateOf(false) }
    var uiVisible by remember { mutableStateOf(true) }

    val modelOptions = listOf("black", "lily", "brownsofa")
    var selectedModel by remember { mutableStateOf(modelOptions.first()) }

    val scannedText: String by QRTextHolder.scannedQRText
    val combinedState: Pair<String, String> by remember(selectedModel, scannedText) {
        derivedStateOf {
            selectedModel to scannedText
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {

            ObjectPlacementIndicator(isReady = objectPlacementReady)
            ARScene(
                modifier = Modifier.fillMaxSize(),
                nodes = nodes,
                planeRenderer = true,
                onCreate = { arSceneView ->
                    arSceneView.lightEstimationMode = Config.LightEstimationMode.DISABLED
                    arSceneView.planeRenderer.isEnabled = true
                    arSceneView.planeRenderer.isShadowReceiver = false
                    arSceneView.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                    modelNode.value =
                        ArModelNode(arSceneView.engine, PlacementMode.BEST_AVAILABLE).apply {

                            loadModelGlbAsync(glbFileLocation = "models/${combinedState}.glb") {
                                // Callback after model is loaded
                            }
                            onAnchorChanged = {
                                placeModelButton.value = !isAnchored
                            }
                            onHitResult = { node, hitResult ->
                                placeModelButton.value = node.isTracking
                            }
                        }
                    nodes.add(modelNode.value!!)
                },
                onSessionCreate = {
                    planeRenderer.isVisible = true
                    planeRenderer.isEnabled = true
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                    objectPlacementReady = true
                }
            )

        // Dropdown menu for model selection
        var uiVisible by remember { mutableStateOf(false) }
        val modelOptions = listOf("Arm Chair, Black Bucket, Sofa")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            // Use DropdownMenu to allow the user to select the model
            DropdownMenu(
                expanded = uiVisible,
                onDismissRequest = { uiVisible = false }
            ) {
                modelOptions.forEach { model ->
                    DropdownMenuItem(text = { model }, onClick = {
                        selectedModel = model
                        QRTextHolder.scannedQRText.value = model
                        uiVisible = false // Close the menu after selecting a model
                    })
                }
            }

            // Button to open the dropdown menu
            Button(
                onClick = { uiVisible = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Text("Choose Model")
            }
        }

        if (uiVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp) // Adjust the padding as needed
            ) {
                // Other content or components if any

                Button(
                    onClick = {
                        horizontalRotationCount++
                        modelNode.value?.let { node ->
                            val rotation = Float3(
                                0f,
                                horizontalRotationCount * (PI.toFloat() / 2),
                                0f
                            ) // 90 degrees per click
                            node.rotation = rotation
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd) // Positions the button at the bottom right
                ) {
                    Text("Rotate 90")
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp) // Adjust the padding as needed
            ) {
                if (placeModelButton.value) {
                    Button(onClick = {
                        modelNode.value?.anchor()
                        objectPlaced = true // Set the indicator to display below the model
                    }, modifier = Modifier.align(Alignment.BottomStart)) {
                        Text(text = "Place")
                    }
                }
            }
        }
        // Button to toggle UI visibility
        Button(
            onClick = { uiVisible = !uiVisible },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text(if (uiVisible) "Hide UI" else "Show UI")
        }
    }

    LaunchedEffect(QRTextHolder.scannedQRText.value) {
        if (QRTextHolder.scannedQRText.value.isNotEmpty()) {
            modelNode.value?.loadModelGlbAsync(glbFileLocation = "models/${QRTextHolder.scannedQRText.value}.glb")
        }
    }
}

data class Chair(var name: String, var imageId: Int)
