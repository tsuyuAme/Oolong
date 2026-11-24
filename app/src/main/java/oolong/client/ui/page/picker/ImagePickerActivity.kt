package oolong.client.ui.page.picker

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import oolong.client.R
import oolong.client.arch.AppContent
import oolong.client.repository.AccountRepository
import oolong.client.toastShort
import oolong.client.ui.page.editor.EditRequest
import oolong.client.ui.page.editor.ImageEditorHost
import oolong.client.ui.page.editor.LocalImageEditor
import oolong.client.ui.widgets.compose.ContextMenuHost
import java.io.File
import javax.inject.Inject

/**
 * Picks pictures from the device without the system photo picker. Returns them as the result's
 * clip data, like the photo picker does.
 */
@AndroidEntryPoint
class ImagePickerActivity : AppCompatActivity() {
    @Inject
    lateinit var accountRepository: AccountRepository

    private val viewModel: ImagePickerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AppContent(this, accountRepository) {
                // Its own window: the album menu and the picture editor open in it
                ContextMenuHost { ImageEditorHost { Content() } }
            }
        }
    }

    @Composable
    private fun Content() {
        val state by viewModel.state.collectAsStateWithLifecycle()
        var access by remember { mutableStateOf(currentAccess()) }
        var asked by rememberSaveable { mutableStateOf(false) }
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            access = currentAccess()
            if (access == PickerAccess.Denied && !shouldShowRequestPermissionRationale(readPermission)) {
                access = PickerAccess.DeniedForever
            }
        }
        // Coming back from the permission dialog or the settings may have changed what is readable
        LifecycleResumeEffect(Unit) {
            val now = currentAccess()
            if (now != PickerAccess.Denied || access != PickerAccess.DeniedForever) access = now
            if (now != PickerAccess.Denied) viewModel.load()
            onPauseOrDispose {}
        }
        LaunchedEffect(Unit) {
            if (access == PickerAccess.Denied && !asked) {
                asked = true
                permissionLauncher.launch(requestedPermissions)
            }
        }
        LaunchedEffect(Unit) {
            viewModel.limitReached.collect { toastShort(R.string.toast_picker_limit, it) }
        }
        // A photo is taken into a file of the picker's own, given back like a picture picked
        var photo by rememberSaveable { mutableStateOf<Uri?>(null) }
        val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
            val uri = photo
            if (taken && uri != null) viewModel.captured(uri)
        }
        val editor = LocalImageEditor.current

        ImagePickerScreen(
            state = state,
            access = access,
            onBack = { finish() },
            onShowAlbum = viewModel::showAlbum,
            onToggle = viewModel::toggle,
            onSelect = viewModel::select,
            onMove = viewModel::move,
            onDone = ::finishWith,
            onTakePhoto = {
                val uri = pickerFile("photo").let(::shared)
                photo = uri
                try {
                    camera.launch(uri)
                } catch (e: ActivityNotFoundException) {
                    toastShort(R.string.toast_picker_no_camera)
                }
            },
            onEdit = { original, place ->
                val output = pickerFile("edit")
                val source = state.edits[original] ?: original
                editor?.open(EditRequest(source, output), from = { place }, back = { place }) { written ->
                    if (written) viewModel.edited(original, shared(output))
                }
            },
            onRequestAccess = { permissionLauncher.launch(requestedPermissions) },
            onOpenSettings = {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                )
            },
        )
    }

    private fun currentAccess(): PickerAccess = when {
        isGranted(readPermission) -> PickerAccess.All
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> PickerAccess.Selected

        else -> PickerAccess.Denied
    }

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    /** A new file for a photo taken or a picture edited here, in the cache the file provider shares. */
    private fun pickerFile(kind: String): File =
        File(cacheDir, "picker").apply { mkdirs() }.let { File(it, "$kind-${System.currentTimeMillis()}.jpg") }

    private fun shared(file: File): Uri = FileProvider.getUriForFile(this, "$packageName.share.FileProvider", file)

    private fun finishWith(uris: List<Uri>) {
        val result = Intent()
        if (uris.isNotEmpty()) {
            result.clipData = ClipData.newRawUri(null, uris.first()).apply {
                uris.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
        }
        setResult(RESULT_OK, result)
        finish()
    }

    companion object {
        private val readPermission: String
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        private val requestedPermissions: Array<String>
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            } else {
                arrayOf(readPermission)
            }

        fun intent(context: Context, maxItems: Int): Intent =
            Intent(context, ImagePickerActivity::class.java).putExtra(ImagePickerViewModel.EXTRA_MAX_ITEMS, maxItems)
    }
}
