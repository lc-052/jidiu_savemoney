package com.jidiu.companion

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

internal object SavingsWords {
    val greetings=listOf("欢迎回来！今天也一起慢慢存钱吧。","先留下一点存款，再安排今天的花销吧。","少一次冲动消费，就多一点实现目标的底气。","不用和别人比，存下适合自己的金额就很好。","今天没存钱也没关系，我们照自己的节奏来。","每一笔真实存款，我都陪你记好。")
    fun thanks(name: String,cents: Long,index: Int)=listOf(
        "谢谢你的$name！\n你又存下了 ¥${formatMoney(cents)}，这笔积累真棒！",
        "收到${name}啦！\n¥${formatMoney(cents)} 留在你的存款里，我们又靠近目标一步。",
        "这份$name，是认真存钱的纪念！\n新存的 ¥${formatMoney(cents)} 已经记好啦。",
        "谢谢你为$name 存下 ¥${formatMoney(cents)}！\n慢慢积累，也能把小目标变成现实。",
    )[index.mod(4)]
}

@Composable
internal fun CustomGiftDialog(onDismiss:()->Unit,onSave:(String,Long)->String?) {
    var name by remember {mutableStateOf("")}
    var amount by remember {mutableStateOf("")}
    var error by remember {mutableStateOf<String?>(null)}
    FormDialog("自定义存钱礼物",onDismiss) {
        Text("写下想送给寄丢的礼物，再定一个每次要新存下的金额。创建礼物不记账，确认赠送时才记录存款。")
        OutlinedTextField(name,{name=it;error=null},label={Text("礼物名称（最多 20 字）")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(amount,{amount=it;error=null},label={Text("每次新存金额（元）")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        Button(onClick={val cents=parseMoney(amount);error=if(cents==null) "请输入大于 0 的金额，最多两位小数" else onSave(name,cents)},modifier=Modifier.fillMaxWidth()) {Text("保存礼物")}
    }
}

@Composable
internal fun SaveLookDialog(onDismiss:()->Unit,onSave:(String)->String?) {
    var name by remember {mutableStateOf("")}
    var error by remember {mutableStateOf<String?>(null)}
    FormDialog("保存当前穿搭",onDismiss) {
        Text("保存衣服、发型、发色和装饰，之后一键换上。保存或使用穿搭不会新增存款。")
        OutlinedTextField(name,{name=it;error=null},label={Text("穿搭名称（最多 16 字）")},singleLine=true,modifier=Modifier.fillMaxWidth())
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        Button(onClick={error=onSave(name)},modifier=Modifier.fillMaxWidth()) {Text("保存穿搭")}
    }
}

@Composable
internal fun BackupControls(store: CompanionStore,onRestored:()->Unit) {
    val context=LocalContext.current
    var feedback by remember {mutableStateOf<String?>(null)}
    var pending by remember {mutableStateOf<String?>(null)}
    var summary by remember {mutableStateOf("")}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {uri ->
        if(uri!=null) try {
            val text=store.exportBackup()
            context.contentResolver.openOutputStream(uri,"wt")!!.bufferedWriter(Charsets.UTF_8).use {it.write(text)}
            feedback="备份已保存，包含账本、物品、穿搭和自定义礼物。"
        } catch(e:Exception) {feedback="备份失败：${e.message}"}
    }
    val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri ->
        if(uri!=null) try {
            val bytes=context.contentResolver.openInputStream(uri)!!.use {input ->
                val output=java.io.ByteArrayOutputStream()
                val buffer=ByteArray(8192)
                while(true) {val count=input.read(buffer);if(count<0) break;require(output.size()+count<=5*1024*1024) {"备份文件过大。"};output.write(buffer,0,count)}
                output.toByteArray()
            }
            require(bytes.size<=5*1024*1024) {"备份文件过大。"}
            val text=bytes.toString(Charsets.UTF_8)
            summary=store.backupSummary(text);pending=text
        } catch(e:Exception) {feedback="无法读取备份。请选择完整的寄丢暂存备份文件（最大 5 MB）。"}
    }
    Text("备份与恢复",style=MaterialTheme.typography.titleMedium)
    Text("把备份文件保存到你选择的位置，换手机后可以导入。")
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick={export.launch("寄丢暂存-${LocalDate.now()}.json")},modifier=Modifier.weight(1f)) {Text("导出备份")}
        OutlinedButton(onClick={restore.launch(arrayOf("application/json","text/plain","application/octet-stream"))},modifier=Modifier.weight(1f)) {Text("导入备份")}
    }
    feedback?.let {Text(it,style=MaterialTheme.typography.bodySmall)}
    if(pending!=null) AlertDialog(onDismissRequest={pending=null},title={Text("确认恢复备份？")},text={Text(summary+"\n建议先导出当前数据备份。恢复失败时当前数据会保持原样。")},dismissButton={TextButton(onClick={pending=null}) {Text("取消")}},confirmButton={TextButton(onClick={
        try {store.restoreBackup(pending!!);pending=null;feedback="备份已恢复。";onRestored()} catch(e:Exception) {pending=null;feedback="恢复失败，当前数据未改变。请检查备份文件是否完整。"}
    }) {Text("替换并恢复")}})
}
