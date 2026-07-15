package com.filerenamer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filerenamer.data.FileItem
import com.filerenamer.data.RenameType
import com.filerenamer.ui.theme.GlassWhite
import com.filerenamer.ui.theme.GlassWhiteStrong

/**
 * 液态玻璃卡片背景
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    blurRadius: Int = 20,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GlassWhite,
                        GlassWhiteStrong.copy(alpha = 0.15f),
                    )
                )
            )
            .graphicsLayer {
                alpha = 0.85f
            }
    ) {
        content()
    }
}

/**
 * 文件/目录项组件
 */
@Composable
fun FileItemCard(
    fileItem: FileItem,
    onToggle: () -> Unit,
    onDoubleClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDragHandle: () -> Modifier = { Modifier },
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (fileItem.isSelected)
            accentColor.copy(alpha = 0.15f)
        else
            Color.Transparent,
        animationSpec = tween(300),
        label = "bgColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (fileItem.isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 12.dp, bottom = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 拖拽手柄
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "拖拽排序",
                modifier = Modifier
                    .size(24.dp)
                    .then(onDragHandle()),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )

            Spacer(modifier = Modifier.width(4.dp))

            // 图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.2f),
                                accentColor.copy(alpha = 0.05f),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (fileItem.isDirectory)
                        Icons.Default.Folder
                    else
                        Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = if (fileItem.isDirectory)
                        Color(0xFFFFB74D)
                    else
                        accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 文件名和信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileItem.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (fileItem.isDirectory) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!fileItem.isDirectory && fileItem.size > 0) {
                    Text(
                        text = formatFileSize(fileItem.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 选择框
            Checkbox(
                checked = fileItem.isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = accentColor,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
        }
    }
}

/**
 * 液态玻璃按钮
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp)),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor.copy(alpha = 0.2f),
            contentColor = accentColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp,
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

/**
 * 顶部操作栏
 */
@Composable
fun TopActionBar(
    title: String,
    onBack: (() -> Unit)? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            actions()
        }
    }
}

/**
 * 底部操作栏
 */
@Composable
fun BottomActionBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onRename: () -> Unit,
    isAllSelected: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isAllSelected,
                    onCheckedChange = { onSelectAll() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = accentColor,
                    )
                )
                Text(
                    text = if (selectedCount > 0) "已选 $selectedCount 项" else "全选",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            GlassButton(
                text = "批量重命名",
                onClick = onRename,
                enabled = selectedCount > 0,
                icon = Icons.Default.Edit,
                accentColor = accentColor,
            )
        }
    }
}

/**
 * 重命名对话框 - 支持10种操作
 */
@Composable
fun RenameDialog(
    visible: Boolean,
    renameText: String,
    renameCharCount: String,
    renamePosition: String,
    renameType: RenameType,
    onTextChange: (String) -> Unit,
    onCharCountChange: (String) -> Unit,
    onPositionChange: (String) -> Unit,
    onTypeChange: (RenameType) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    text = "批量重命名",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // 操作类型选择 - 五行
                    Text(
                        text = "操作类型",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 第一行：添加前缀/后缀
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = renameType == RenameType.ADD_PREFIX,
                            onClick = { onTypeChange(RenameType.ADD_PREFIX) },
                            label = { Text("添加前缀", fontSize = 13.sp) },
                            leadingIcon = if (renameType == RenameType.ADD_PREFIX) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = accentColor,
                            )
                        )
                        FilterChip(
                            selected = renameType == RenameType.ADD_SUFFIX,
                            onClick = { onTypeChange(RenameType.ADD_SUFFIX) },
                            label = { Text("添加后缀", fontSize = 13.sp) },
                            leadingIcon = if (renameType == RenameType.ADD_SUFFIX) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = accentColor,
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 第二行：删除前N个/后N个
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = renameType == RenameType.REMOVE_FIRST_N,
                            onClick = { onTypeChange(RenameType.REMOVE_FIRST_N) },
                            label = { Text("删除前N个", fontSize = 13.sp) },
                            leadingIcon = if (renameType == RenameType.REMOVE_FIRST_N) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = accentColor,
                            )
                        )
                        FilterChip(
                            selected = renameType == RenameType.REMOVE_LAST_N,
                            onClick = { onTypeChange(RenameType.REMOVE_LAST_N) },
                            label = { Text("删除后N个", fontSize = 13.sp) },
                            leadingIcon = if (renameType == RenameType.REMOVE_LAST_N) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = accentColor,
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 第三行：替换前N个/后N个
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = renameType == RenameType.REPLACE_FIRST_N,
                            onClick = { onTypeChange(RenameType.REPLACE_FIRST_N) },
                            label = { Text("替换前N个", fontSize = 13.sp) },
                            leadingIcon = if (renameType == RenameType.REPLACE_FIRST_N) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = accentColor,
                            )
                        )
                        FilterChip(
                            selected = renameType == RenameType.REPLACE_LAST_N,
                            onClick = { onTypeChange(RenameType.REPLACE_LAST_N) },
                            label = { Text("替换后N个", fontSize = 13.sp) },
                            leadingIcon = if (renameType == RenameType.REPLACE_LAST_N) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = accentColor,
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 第四行：从前往后第N位插入/删除
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = renameType == RenameType.INSERT_AT_N_FROM_START,
                            onClick = { onTypeChange(RenameType.INSERT_AT_N_FROM_START) },
                            label = { Text("前→后第N位插入", fontSize = 12.sp) },
                            leadingIcon = if (renameType == RenameType.INSERT_AT_N_FROM_START) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = accentColor,
                            )
                        )
                        FilterChip(
                            selected = renameType == RenameType.DELETE_AT_N_FROM_START,
                            onClick = { onTypeChange(RenameType.DELETE_AT_N_FROM_START) },
                            label = { Text("前→后第N位删除", fontSize = 12.sp) },
                            leadingIcon = if (renameType == RenameType.DELETE_AT_N_FROM_START) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = accentColor,
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 第五行：从后往前第N位插入/删除
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = renameType == RenameType.INSERT_AT_N_FROM_END,
                            onClick = { onTypeChange(RenameType.INSERT_AT_N_FROM_END) },
                            label = { Text("后→前第N位插入", fontSize = 12.sp) },
                            leadingIcon = if (renameType == RenameType.INSERT_AT_N_FROM_END) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = accentColor,
                            )
                        )
                        FilterChip(
                            selected = renameType == RenameType.DELETE_AT_N_FROM_END,
                            onClick = { onTypeChange(RenameType.DELETE_AT_N_FROM_END) },
                            label = { Text("后→前第N位删除", fontSize = 12.sp) },
                            leadingIcon = if (renameType == RenameType.DELETE_AT_N_FROM_END) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = accentColor,
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 第六行：添加前缀id号/后缀id号
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = renameType == RenameType.ADD_ID_PREFIX,
                            onClick = { onTypeChange(RenameType.ADD_ID_PREFIX) },
                            label = { Text("前缀id号", fontSize = 13.sp) },
                            leadingIcon = if (renameType == RenameType.ADD_ID_PREFIX) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = accentColor,
                            )
                        )
                        FilterChip(
                            selected = renameType == RenameType.ADD_ID_SUFFIX,
                            onClick = { onTypeChange(RenameType.ADD_ID_SUFFIX) },
                            label = { Text("后缀id号", fontSize = 13.sp) },
                            leadingIcon = if (renameType == RenameType.ADD_ID_SUFFIX) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = accentColor,
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 输入区域
                    when (renameType) {
                        RenameType.ADD_PREFIX, RenameType.ADD_SUFFIX -> {
                            OutlinedTextField(
                                value = renameText,
                                onValueChange = onTextChange,
                                label = {
                                    Text(
                                        if (renameType == RenameType.ADD_PREFIX)
                                            "输入前缀文本" else "输入后缀文本"
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    cursorColor = accentColor,
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (renameType == RenameType.ADD_PREFIX)
                                    "示例: 输入 \"笔记_\" → \"笔记_文件名.pdf\""
                                else
                                    "示例: 输入 \"_v2\" → \"文件名_v2.pdf\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        RenameType.REMOVE_FIRST_N, RenameType.REMOVE_LAST_N -> {
                            OutlinedTextField(
                                value = renameCharCount,
                                onValueChange = onCharCountChange,
                                label = {
                                    Text(
                                        if (renameType == RenameType.REMOVE_FIRST_N)
                                            "删除前几个字符？" else "删除后几个字符？"
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    cursorColor = accentColor,
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (renameType == RenameType.REMOVE_FIRST_N)
                                    "示例: 输入 3 → \"ABCDE.pdf\" → \"DE.pdf\""
                                else
                                    "示例: 输入 3 → \"ABCDE.pdf\" → \"AB.pdf\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        RenameType.REPLACE_FIRST_N, RenameType.REPLACE_LAST_N -> {
                            // 替换字符数输入
                            OutlinedTextField(
                                value = renameCharCount,
                                onValueChange = onCharCountChange,
                                label = {
                                    Text(
                                        if (renameType == RenameType.REPLACE_FIRST_N)
                                            "替换前几个字符？" else "替换后几个字符？"
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    cursorColor = accentColor,
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 替换文本输入
                            OutlinedTextField(
                                value = renameText,
                                onValueChange = onTextChange,
                                label = { Text("替换为") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    cursorColor = accentColor,
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (renameType == RenameType.REPLACE_FIRST_N)
                                    "示例: 替换前3个字符为\"XX\" → \"ABCDE.pdf\" → \"XXDE.pdf\""
                                else
                                    "示例: 替换后3个字符为\"XX\" → \"ABCDE.pdf\" → \"ABXX.pdf\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        // ===== 新增功能：从前往后第N位插入 =====
                        RenameType.INSERT_AT_N_FROM_START -> {
                            // 位置输入
                            OutlinedTextField(
                                value = renamePosition,
                                onValueChange = onPositionChange,
                                label = { Text("从前往后第几位插入？（从0开始）") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    cursorColor = accentColor,
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 插入文本输入
                            OutlinedTextField(
                                value = renameText,
                                onValueChange = onTextChange,
                                label = { Text("要插入的文本") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    cursorColor = accentColor,
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "示例: 位置2, 插入\"XX\" → \"ABCDE.pdf\" → \"ABXXCDE.pdf\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        // ===== 新增功能：从前往后第N位删除 =====
                        RenameType.DELETE_AT_N_FROM_START -> {
                            // 位置输入
                            OutlinedTextField(
                                value = renamePosition,
                                onValueChange = onPositionChange,
                                label = { Text("从前往后第几位开始删除？（从0开始）") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    cursorColor = accentColor,
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 删除字符数输入
                            OutlinedTextField(
                                value = renameCharCount,
                                onValueChange = onCharCountChange,
                                label = { Text("删除几个字符？") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    cursorColor = accentColor,
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "示例: 位置1, 删除2个 → \"ABCDE.pdf\" → \"ADE.pdf\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        // ===== 新增功能：从后往前第N位插入 =====
                        RenameType.INSERT_AT_N_FROM_END -> {
                            // 位置输入
                            OutlinedTextField(
                                value = renamePosition,
                                onValueChange = onPositionChange,
                                label = { Text("从后往前第几位插入？（从0开始）") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    cursorColor = accentColor,
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 插入文本输入
                            OutlinedTextField(
                                value = renameText,
                                onValueChange = onTextChange,
                                label = { Text("要插入的文本") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    cursorColor = accentColor,
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "示例: 位置2, 插入\"XX\" → \"ABCDE.pdf\" → \"ABCXXDE.pdf\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        // ===== 新增功能：从后往前第N位删除 =====
                        RenameType.DELETE_AT_N_FROM_END -> {
                            // 位置输入
                            OutlinedTextField(
                                value = renamePosition,
                                onValueChange = onPositionChange,
                                label = { Text("从后往前第几位开始删除？（从0开始）") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    cursorColor = accentColor,
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 删除字符数输入
                            OutlinedTextField(
                                value = renameCharCount,
                                onValueChange = onCharCountChange,
                                label = { Text("删除几个字符？") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    cursorColor = accentColor,
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "示例: 位置2, 删除2个 → \"ABCDE.pdf\" → \"ABE.pdf\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        // ===== 新增：添加前缀id号 =====
                        RenameType.ADD_ID_PREFIX -> {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "按文件列表顺序自动添加前缀id号（01_、02_...）",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "示例: 01_文件名.pdf, 02_文件名.pdf",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        // ===== 新增：添加后缀id号 =====
                        RenameType.ADD_ID_SUFFIX -> {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "按文件列表顺序自动添加后缀id号（_01、_02...）",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "示例: 文件名_01.pdf, 文件名_02.pdf",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.2f),
                        contentColor = accentColor,
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("开始重命名")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 格式化文件大小
 */
fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
        size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
    }
}
