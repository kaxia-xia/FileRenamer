package com.filerenamer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.filerenamer.data.RenameType
import com.filerenamer.ui.components.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    uiState: MainUiState,
    onSelectFolder: () -> Unit,
    onEnterDirectory: (com.filerenamer.data.FileItem) -> Unit,
    onGoBack: () -> Unit,
    onToggleFile: (com.filerenamer.data.FileItem) -> Unit,
    onToggleSelectAll: () -> Unit,
    onReorderFiles: (Int, Int) -> Unit,
    onShowRenameDialog: () -> Unit,
    onHideRenameDialog: () -> Unit,
    onRenameTextChange: (String) -> Unit,
    onRenameCharCountChange: (String) -> Unit,
    onRenamePositionChange: (String) -> Unit,
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
                        DraggableFileList(
                            files = uiState.files,
                            onToggleFile = onToggleFile,
                            onEnterDirectory = onEnterDirectory,
                            onReorder = onReorderFiles,
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

                // 结果提示（含失败详情）
                uiState.renameResult?.let { result ->
                    RenameResultCard(
                        summary = result,
                        failDetails = uiState.renameFailDetails,
                        onDismiss = onClearResult,
                        accentColor = accentColor,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
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
            renamePosition = uiState.renamePosition,
            renameType = uiState.renameType,
            onTextChange = onRenameTextChange,
            onCharCountChange = onRenameCharCountChange,
            onPositionChange = onRenamePositionChange,
            onTypeChange = onRenameTypeChange,
            onConfirm = onExecuteRename,
            onDismiss = onHideRenameDialog,
            accentColor = accentColor,
        )
    }
}

/**
 * 可拖拽排序的文件列表 - 支持连续跨位拖动和边缘自动滚动
 *
 * 使用 rememberUpdatedState 确保 pointerInput 内部始终拿到最新的 files 和 onReorder 引用。
 * 每次 onDrag 只交换一个位置，交换后 dragOffset 置零，
 * 下一帧重组后继续判断，从而实现连续跨位拖动。
 */
@Composable
private fun DraggableFileList(
    files: List<com.filerenamer.data.FileItem>,
    onToggleFile: (com.filerenamer.data.FileItem) -> Unit,
    onEnterDirectory: (com.filerenamer.data.FileItem) -> Unit,
    onReorder: (Int, Int) -> Unit,
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

    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // 用 rememberUpdatedState 持有最新引用，pointerInput 内部始终能读到最新的值
    val currentFiles by rememberUpdatedState(files)
    val currentOnReorder by rememberUpdatedState(onReorder)

    // 拖拽状态
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    // 手指在item内的Y坐标（用于边缘滚动检测）
    var fingerY by remember { mutableFloatStateOf(0f) }

    // 自动滚动控制
    var isAutoScrolling by remember { mutableStateOf(false) }
    var scrollDirection by remember { mutableIntStateOf(0) }

    // 自动滚动协程 - 持续运行，检查状态决定是否滚动
    LaunchedEffect(Unit) {
        while (true) {
            if (isAutoScrolling && draggedIndex >= 0 && scrollDirection != 0) {
                val scrollPx = scrollDirection * 12
                listState.dispatchRawDelta(scrollPx.toFloat())
            }
            delay(16)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            Text(
                text = "共 ${files.size} 项（长按拖动排序）",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        itemsIndexed(files, key = { _, file -> file.uri.toString() }) { index, file ->
            val isDragging = draggedIndex == index

            val offsetY by animateDpAsState(
                targetValue = if (isDragging) with(density) { dragOffset.toDp() } else 0.dp,
                label = "offsetY"
            )

            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = offsetY.toPx()
                        scaleX = if (isDragging) 1.05f else 1f
                        scaleY = if (isDragging) 1.05f else 1f
                        shadowElevation = if (isDragging) 10f else 0f
                    }
                    .pointerInput(index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedIndex = index
                                dragOffset = 0f
                                fingerY = 0f
                                isAutoScrolling = false
                                scrollDirection = 0
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (draggedIndex < 0) return@detectDragGesturesAfterLongPress

                                // 记录手指在item内的位置
                                fingerY = change.position.y

                                dragOffset += dragAmount.y

                                // 获取当前拖拽item的布局信息
                                val layoutInfo = listState.layoutInfo
                                val currentItem = layoutInfo.visibleItemsInfo.find { it.index == draggedIndex }

                                if (currentItem != null) {
                                    val itemHeight = currentItem.size

                                    // === 单步交换逻辑 ===
                                    // 每次 onDrag 最多交换一个位置，交换后重置偏移量，
                                    // 下一帧重组后 draggedIndex 已更新，继续判断
                                    if (itemHeight > 0) {
                                        // 向下拖动：偏移超过一个item高度就交换到下一位置
                                        if (dragOffset > itemHeight) {
                                            val targetIdx = draggedIndex + 1
                                            if (targetIdx < currentFiles.size) {
                                                currentOnReorder(draggedIndex, targetIdx)
                                                draggedIndex = targetIdx
                                                dragOffset = 0f
                                            }
                                        }
                                        // 向上拖动：偏移超过一个item高度就交换到上一位置
                                        else if (dragOffset < -itemHeight) {
                                            val targetIdx = draggedIndex - 1
                                            if (targetIdx >= 0) {
                                                currentOnReorder(draggedIndex, targetIdx)
                                                draggedIndex = targetIdx
                                                dragOffset = 0f
                                            }
                                        }
                                    }

                                    // === 边缘自动滚动检测（基于手指全局位置） ===
                                    val viewportHeight = layoutInfo.viewportEndOffset
                                    val edgeThreshold = with(density) { 100.dp.toPx() }

                                    // 手指在屏幕上的全局Y坐标
                                    val fingerGlobalY = currentItem.offset + fingerY

                                    val shouldScrollUp = fingerGlobalY < edgeThreshold &&
                                            layoutInfo.visibleItemsInfo.firstOrNull()?.index != 0
                                    val shouldScrollDown = fingerGlobalY > viewportHeight - edgeThreshold &&
                                            layoutInfo.visibleItemsInfo.lastOrNull()?.let { it.index + 1 } != layoutInfo.totalItemsCount

                                    if (shouldScrollUp) {
                                        if (!isAutoScrolling || scrollDirection != -1) {
                                            isAutoScrolling = true
                                            scrollDirection = -1
                                        }
                                    } else if (shouldScrollDown) {
                                        if (!isAutoScrolling || scrollDirection != 1) {
                                            isAutoScrolling = true
                                            scrollDirection = 1
                                        }
                                    } else {
                                        isAutoScrolling = false
                                        scrollDirection = 0
                                    }
                                }
                            },
                            onDragEnd = {
                                draggedIndex = -1
                                dragOffset = 0f
                                fingerY = 0f
                                isAutoScrolling = false
                                scrollDirection = 0
                            },
                            onDragCancel = {
                                draggedIndex = -1
                                dragOffset = 0f
                                fingerY = 0f
                                isAutoScrolling = false
                                scrollDirection = 0
                            }
                        )
                    }
            ) {
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
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 重命名结果卡片 - 显示概要并可展开查看失败详情
 */
@Composable
private fun RenameResultCard(
    summary: String,
    failDetails: List<String>,
    onDismiss: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (failDetails.isEmpty())
                accentColor.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 概要行
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (failDetails.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (failDetails.isEmpty()) accentColor else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (failDetails.isEmpty())
                        accentColor
                    else
                        MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) {
                    Text("关闭", fontSize = 13.sp)
                }
            }

            // 如果有失败详情，显示展开/折叠按钮
            if (failDetails.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.padding(start = 34.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (expanded) "收起失败详情" else "查看失败详情（${failDetails.size}条）",
                        fontSize = 13.sp
                    )
                }

                // 失败详情列表（可展开）
                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .padding(start = 34.dp, top = 4.dp)
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        failDetails.forEach { detail ->
                            Text(
                                text = "• $detail",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }
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
                                "4. 长按文件可拖动排序列表\n" +
                                "5. 点击「批量重命名」选择操作类型\n" +
                                "6. 支持：添加前缀、添加后缀、删除前N个、\n" +
                                "   删除后N个、替换前N个、替换后N个、\n" +
                                "   从前往后第N位插入、从前往后第N位删除、\n" +
                                "   从后往前第N位插入、从后往前第N位删除",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}
