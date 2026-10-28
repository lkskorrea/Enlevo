package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.FamilyDocument
import com.example.data.model.FamilyPhoto
import com.example.data.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private suspend fun encodeUriToCompressedBase64(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
            val maxDim = 640
            val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
            val targetW = (original.width * ratio).toInt().coerceAtLeast(1)
            val targetH = (original.height * ratio).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(original, targetW, targetH, true)
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 72, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

private fun decodeBase64ToImageBitmap(base64Str: String): ImageBitmap? {
    return try {
        val clean = base64Str.substringAfter(",")
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

@Composable
fun PhotosScreen(
    photosState: UiState<List<FamilyPhoto>>,
    onAddPhoto: (String, String) -> Unit,
    onUpdateCaption: (String, String) -> Unit,
    onDeletePhoto: (String) -> Unit,
    onFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var caption by remember { mutableStateOf("") }
    var selectedBase64 by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var selectedPhotoForViewer by remember { mutableStateOf<FamilyPhoto?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            scope.launch {
                val encoded = encodeUriToCompressedBase64(context, uri)
                isProcessing = false
                if (encoded != null) {
                    selectedBase64 = encoded
                } else {
                    onFeedback("Não foi possível processar a imagem selecionada.")
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("photos_screen"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Publicar no Álbum da Família",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_photo_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (selectedBase64 != null) "Foto selecionada (Toque para trocar)"
                            else "Selecionar Foto da Galeria"
                        )
                    }

                    if (selectedBase64 != null) {
                        val previewBitmap = remember(selectedBase64) {
                            decodeBase64ToImageBitmap(selectedBase64!!)
                        }
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap,
                                contentDescription = "Prévia da foto selecionada",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("Legenda ou Álbum da foto") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("photo_caption_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            val base64 = selectedBase64
                            if (!base64.isNullOrBlank()) {
                                onAddPhoto(caption.ifBlank { "Momento em Família" }, base64)
                                caption = ""
                                selectedBase64 = null
                            }
                        },
                        enabled = !selectedBase64.isNullOrBlank() && !isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_photo_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("A processar imagem...")
                        } else {
                            Text("Carregar Foto")
                        }
                    }
                }
            }
        }

        when (photosState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item { ErrorBanner(photosState.message) }
            }
            is UiState.Success -> {
                if (photosState.data.isEmpty()) {
                    item {
                        EmptyStateCard(
                            emoji = "🖼️",
                            title = "Álbum de fotos vazio",
                            subtitle = "Selecione uma foto da galeria acima para compartilhar momentos especiais com a família!"
                        )
                    }
                } else {
                    items(photosState.data.chunked(2)) { rowPhotos ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowPhotos.forEach { photo ->
                                val bitmap = remember(photo.id, photo.imageBase64) {
                                    decodeBase64ToImageBitmap(photo.imageBase64)
                                }
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedPhotoForViewer = photo },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap,
                                                contentDescription = photo.caption,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(12.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("🖼️")
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = photo.caption,
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Por ${photo.authorName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (rowPhotos.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedPhotoForViewer != null) {
        val current = selectedPhotoForViewer!!
        var editCaption by remember(current) { mutableStateOf(current.caption) }
        val bitmap = remember(current.id, current.imageBase64) {
            decodeBase64ToImageBitmap(current.imageBase64)
        }

        AlertDialog(
            onDismissRequest = { selectedPhotoForViewer = null },
            title = { Text(current.caption) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = current.caption,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        text = "Publicada por ${current.authorName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editCaption,
                        onValueChange = { editCaption = it },
                        label = { Text("Editar legenda") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateCaption(current.id, editCaption)
                        selectedPhotoForViewer = null
                    }
                ) {
                    Text("Salvar Legenda")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onDeletePhoto(current.id)
                            selectedPhotoForViewer = null
                        }
                    ) {
                        Text("Excluir", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { selectedPhotoForViewer = null }) {
                        Text("Fechar")
                    }
                }
            }
        )
    }
}

@Composable
fun DocumentsScreen(
    documentsState: UiState<List<FamilyDocument>>,
    onAddDocument: (String, String, String, String) -> Unit,
    onUpdateDocument: (String, String, String, String, String) -> Unit,
    onDeleteDocument: (String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var notesOrUrl by remember { mutableStateOf("") }
    var attachedFileName by remember { mutableStateOf("") }
    var attachedFileSummary by remember { mutableStateOf("") }
    var docToEdit by remember { mutableStateOf<FamilyDocument?>(null) }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var displayName = "arquivo_anexo"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    displayName = cursor.getString(nameIndex) ?: "arquivo_anexo"
                }
            }
            attachedFileName = displayName
            if (title.isBlank()) {
                title = displayName.substringBeforeLast(".")
            }
            val textPreview = try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = ByteArray(2048)
                    val read = stream.read(bytes)
                    if (read > 0) String(bytes, 0, read, Charsets.UTF_8).filter { it.isLetterOrDigit() || it.isWhitespace() || it in ".,;:-_/@()" }.take(600)
                    else ""
                } ?: ""
            } catch (e: Exception) {
                ""
            }
            attachedFileSummary = textPreview.ifBlank { "Arquivo selecionado: $displayName" }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("documents_screen"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Guardar Documento da Família",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedButton(
                        onClick = { docPickerLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (attachedFileName.isNotBlank()) "Anexo: $attachedFileName"
                            else "Selecionar Ficheiro do Aparelho (Opcional)"
                        )
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nome do documento (ex: Conta de Luz, Receita Médica)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("doc_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = notesOrUrl,
                        onValueChange = { notesOrUrl = it },
                        label = { Text("Link (Google Drive / Site) ou Dados / Código PIX") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("doc_notes_input"),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onAddDocument(
                                    title,
                                    notesOrUrl,
                                    attachedFileName,
                                    attachedFileSummary
                                )
                                title = ""
                                notesOrUrl = ""
                                attachedFileName = ""
                                attachedFileSummary = ""
                            }
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_document_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Guardar Documento")
                    }
                }
            }
        }

        when (documentsState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item { ErrorBanner(documentsState.message) }
            }
            is UiState.Success -> {
                if (documentsState.data.isEmpty()) {
                    item {
                        EmptyStateCard(
                            emoji = "📄",
                            title = "Nenhum documento guardado",
                            subtitle = "Salve documentos, comprovantes, números de apólices ou links importantes da família."
                        )
                    }
                } else {
                    items(documentsState.data, key = { it.id }) { doc ->
                        val isUrl = doc.notesOrUrl.startsWith("http://") || doc.notesOrUrl.startsWith("https://")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "📄 ${doc.title}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = buildString {
                                                append("Por ${doc.authorName}")
                                                if (doc.fileName.isNotBlank()) {
                                                    append(" • Ficheiro: ${doc.fileName}")
                                                }
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = { docToEdit = doc }) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Editar documento",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { onDeleteDocument(doc.id) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Excluir documento",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                if (doc.notesOrUrl.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = doc.notesOrUrl,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (isUrl) {
                                        Button(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(doc.notesOrUrl))
                                                context.startActivity(intent)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Abrir / Baixar")
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "${doc.title}\n${doc.notesOrUrl}\n${doc.fileSummary}".trim()
                                                )
                                            }
                                            context.startActivity(
                                                Intent.createChooser(shareIntent, "Compartilhar ${doc.title}")
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Compartilhar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (docToEdit != null) {
        val current = docToEdit!!
        var editTitle by remember(current) { mutableStateOf(current.title) }
        var editNotes by remember(current) { mutableStateOf(current.notesOrUrl) }
        AlertDialog(
            onDismissRequest = { docToEdit = null },
            title = { Text("Editar Documento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Nome do documento") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Link ou Observações") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateDocument(current.id, editTitle, editNotes, current.fileName, current.fileSummary)
                        docToEdit = null
                    },
                    enabled = editTitle.isNotBlank()
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { docToEdit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
