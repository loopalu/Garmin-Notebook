package ee.loopalu.garmin.notebook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import ee.loopalu.garmin.notebook.presentation.NotebookViewModel
import ee.loopalu.garmin.notebook.presentation.ui.NotebookApp

class MainActivity : ComponentActivity() {
    private val viewModel: NotebookViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                NotebookApp(viewModel)
            }
        }
    }
}
