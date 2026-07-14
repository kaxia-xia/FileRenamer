package com.filerenamer.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filerenamer.data.RenameType
import com.filerenamer.ui.components.*

@Composable
fun MainScreen(
    uiState: MainUiState,
    onSelectFolder: () -> Unit,
    onEnterDirectory: (com.filerenamer.data.FileItem) -> Unit,
    onGoBack: () -> Unit,
    onToggleFile: (com.filerenamer.data.FileItem) -> Unit,
    onToggleSelectAll: () -> Unit,
    onShowRenameDialog: () -> Unit,
    onHideRenameDialog: () -> Unit,
    onRenameTextChange: (String) -> Unit,
    onRenameCharCountChange: (String) -> Unit,
    onRenameTypeChange: (RenameType) -> Unit,
    onExecuteRename: () -> Unit,
    onClearError: () -> Unit,
    onClearResult: () -> Unit,
) {
    val accentColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏
            if (uiState.isInitialState) {
                TopActionBar(
                    title = "文件批量重命名",
                    onBack = null,
                    accentColor = accentColor,
                )
            } else {
                TopActionBar(
                    title = uiState.currentDirectoryName,
                    onBack = { onGoBack() },
                    accentColor = accentColor,
                ) {
                    IconButton(onClick = {
                        uiState.currentDirectoryUri?.let { uri ->
                            // 刷新由ViewModel处理
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 内容区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    uiState.isInitialState -> {
                        InitialContent(
                            onSelectFolder = onSelectFolder,
                            accentColor = accentColor,
                        )
                    }
                    uiState.isLoading || uiState.isRenaming -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = accentColor,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (uiState.isRenaming) "正在重命名..." else "加载中...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        FileListContent(
                            files = uiState.files,
                            onToggleFile = onToggleFile,
                            onEnterDirectory = onEnterDirectory,
                            accentColor = accentColor,
                        )
                    }
                }

                // 错误提示
                uiState.errorMessage?.let { error ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = onClearError) {
                                Text("关闭")
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(error)
                    }
                }

                // 结果提示
                uiState.renameResult?.let { result ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = onClearResult) {
                                Text("关闭")
                            }
                        },
                        containerColor = accentColor.copy(alpha = 0.2f),
                        contentColor = accentColor,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(result)
                    }
                }
            }

            // 底部操作栏
            if (!uiState.isInitialState && !uiState.isLoading && !uiState.isRenaming) {
                BottomActionBar(
                    selectedCount = uiState.selectedCount,
                    onSelectAll = onToggleSelectAll,
                    onRename = onShowRenameDialog,
                    isAllSelected = uiState.isAllSelected,
                    accentColor = accentColor,
                )
            }
        }

        // 重命名对话框
        RenameDialog(
            visible = uiState.isRenameDialogVisible,
            renameText = uiState.renameText,
            renameCharCount = uiState.renameCharCount,
            renameType = uiState.renameType,
            onTextChange = onRenameTextChange,
            onCharCountChange = onRenameCharCountChange,
            onTypeChange = onRenameTypeChange,
            onConfirm = onExecuteRename,
            onDismiss = onHideRenameDialog,
            accentColor = accentColor,
        )
    }
}

/**
 * 初始页面 - 选择文件夹
 */
@Composable
private fun InitialContent(
    onSelectFolder: () -> Unit,
    accentColor: Color,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // 大图标
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.3f),
                                accentColor.copy(alpha = 0.1f),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "文件批量重命名",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "选择一个文件夹，批量添加前缀/后缀\n删除前N个/后N个字符，或替换前N个/后N个字符\n支持内部存储、U盘、SD卡",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onSelectFolder,
                modifier = Modifier
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor.copy(alpha = 0.2f),
                    contentColor = accentColor,
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "选择文件夹",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = accentColor.copy(alpha = 0.05f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. 点击「选择文件夹」按钮\n" +
                                "2. 在系统文件选择器中找到目标文件夹\n" +
                                "3. 浏览文件夹，勾选要重命名的文件和文件夹\n" +
                                "4. 点击「批量重命名」选择操作类型\n" +
                                "5. 支持：添加前缀、添加后缀、删除前N个字符、\n" +
                                "   删除后N个字符、替换前N个字符、替换后N个字符",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun FileListContent(
    files: List<com.filerenamer.data.FileItem>,
    onToggleFile: (com.filerenamer.data.FileItem) -> Unit,
    onEnterDirectory: (com.filerenamer.data.FileItem) -> Unit,
    accentColor: Color,
) {
    if (files.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "此目录为空",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            Text(
                text = "共 ${files.size} 项",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(files, key = { it.uri.toString() }) { file ->
            FileItemCard(
                fileItem = file,
                onToggle = { onToggleFile(file) },
                onDoubleClick = {
                    if (file.isDirectory) {
                        onEnterDirectory(file)
                    }
                },
                accentColor = accentColor,
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
