package com.jidiu.companion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Cream = Color(0xFFF8F6F0)
private val Forest = Color(0xFF556957)
private val Apricot = Color(0xFFEFC4AD)
private val Muted = Color(0xFF7E827B)
private val PaleGreen = Color(0xFFE7ECE4)
private val Paper = Color(0xFFFFFDF8)
private data class RewardSelection(val item: VillageItem? = null, val gift: Gift? = null) {
    val name get() = item?.name ?: gift!!.name
    val cents get() = item?.priceCents ?: gift!!.priceCents
}

@Composable
fun CompanionApp(store: CompanionStore) {
    var snapshot by remember(store) { mutableStateOf(store.snapshot()) }
    var tab by rememberSaveable { mutableStateOf(0) }
    var entryDialog by remember { mutableStateOf<EntryKind?>(null) }
    var rewardSelection by remember { mutableStateOf<RewardSelection?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }
    var customGiftOpen by remember {mutableStateOf(false)}
    var saveLookOpen by remember {mutableStateOf(false)}
    var deletingEntry by remember { mutableStateOf<MoneyEntry?>(null) }
    var celebrating by remember { mutableStateOf(false) }
    var speech by remember { mutableStateOf<String?>(null) }
    var reactionId by remember { mutableStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun change(message: String, action: () -> Unit): String? = try {
        action(); snapshot = store.snapshot()
        scope.launch { snackbar.showSnackbar(message) }; null
    } catch(error: Exception) { error.message ?: "操作失败，请稍后再试" }
    fun react(text: String, thanks: Boolean = false) { speech = text; celebrating = thanks; reactionId++ }
    LaunchedEffect(reactionId) {
        if(reactionId > 0) { delay(5000); speech = null; celebrating = false }
    }
    BackHandler(enabled = tab != 0 && entryDialog == null && rewardSelection == null && !settingsOpen && deletingEntry == null) { tab = 0 }
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Forest, onPrimary = Color.White, primaryContainer = PaleGreen,
            onPrimaryContainer = Forest, secondary = Color(0xFFA57B63), secondaryContainer = Color(0xFFF5E4D7),
            onSecondaryContainer = Forest, background = Cream, onBackground = Forest, surface = Paper,
            onSurface = Forest, surfaceVariant = PaleGreen, onSurfaceVariant = Muted, outline = Color(0xFFC4CDC0), error = Color(0xFFAF453F)),
        shapes = androidx.compose.material3.Shapes(small = RoundedCornerShape(2.dp), medium = RoundedCornerShape(4.dp), large = RoundedCornerShape(6.dp)),
        typography = Typography(
            headlineLarge = TextStyle(fontSize=28.sp,lineHeight=36.sp,fontWeight=FontWeight.Bold),
            headlineMedium = TextStyle(fontSize=24.sp,lineHeight=32.sp,fontWeight=FontWeight.Bold),
            titleLarge = TextStyle(fontSize=20.sp,lineHeight=28.sp,fontWeight=FontWeight.SemiBold),
            titleMedium = TextStyle(fontSize=16.sp,lineHeight=24.sp,fontWeight=FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize=16.sp,lineHeight=25.sp), bodyMedium = TextStyle(fontSize=14.sp,lineHeight=22.sp),
            bodySmall = TextStyle(fontSize=12.sp,lineHeight=18.sp), labelLarge = TextStyle(fontSize=14.sp,lineHeight=20.sp,fontWeight=FontWeight.SemiBold)),
    ) {
        Surface(color=Cream,modifier=Modifier.fillMaxSize()) {
            Scaffold(modifier=Modifier.safeDrawingPadding(),containerColor=Cream,contentWindowInsets=WindowInsets(0,0,0,0),
                topBar={ Row(Modifier.fillMaxWidth().padding(start=20.dp,end=8.dp,top=8.dp,bottom=4.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).background(PaleGreen,RoundedCornerShape(3.dp)),contentAlignment=Alignment.Center) { Text("寄",fontWeight=FontWeight.Bold) }
                    Spacer(Modifier.width(10.dp)); Text("寄丢暂存",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
                    TextButton(onClick={settingsOpen=true}) { Text("设置") }
                } },
                bottomBar={ NavigationBar(containerColor=Paper,tonalElevation=0.dp,windowInsets=WindowInsets(0,0,0,0)) {
                    listOf("村庄" to "⌂","账本" to "≡","物品" to "▦","礼物" to "♡").forEachIndexed { i,pair ->
                        NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Text(pair.second,fontSize=23.sp)},label={Text(pair.first)},
                            colors=NavigationBarItemDefaults.colors(selectedIconColor=Forest,selectedTextColor=Forest,indicatorColor=PaleGreen,unselectedIconColor=Muted,unselectedTextColor=Muted))
                    }
                } },snackbarHost={SnackbarHost(snackbar)}) { padding ->
                Box(Modifier.fillMaxSize().padding(padding),contentAlignment=Alignment.TopCenter) {
                    val page=Modifier.widthIn(max=700.dp).fillMaxSize()
                    when(tab) {
                        0 -> HomePage(snapshot,celebrating,speech,
                            onPetTap={react(SavingsWords.greetings[reactionId % SavingsWords.greetings.size])},
                            onSave={entryDialog=EntryKind.SAVING},onRecord={entryDialog=EntryKind.EXPENSE},onItems={tab=2},onGifts={tab=3},modifier=page)
                        1 -> LedgerPage(snapshot,onRecord={entryDialog=EntryKind.EXPENSE},onWithdraw={entryDialog=EntryKind.WITHDRAWAL},onDelete={deletingEntry=it},modifier=page)
                        2 -> ItemsPage(snapshot,onAcquire={rewardSelection=RewardSelection(item=it)},onToggle={ item ->
                            val error=change("装扮已更新") { store.toggleItem(item.id) }
                            if(error!=null) scope.launch { snackbar.showSnackbar(error) }
                        },onHairColor={hairId,color ->
                            val error=change("发色已更新") {store.selectHairColor(hairId,color)}
                            if(error!=null) scope.launch {snackbar.showSnackbar(error)}
                        },onSaveLook={saveLookOpen=true},onApplyLook={id ->
                            val error=change("穿搭已换好，没有新增存款") {store.applyLook(id)}
                            if(error!=null) scope.launch {snackbar.showSnackbar(error)}
                        },modifier=page)
                        3 -> GiftsPage(snapshot,onGift={rewardSelection=RewardSelection(gift=it)},onCustomGift={customGiftOpen=true},modifier=page)
                    }
                }
            }
            rewardSelection?.let { selection -> RewardDepositDialog(selection,onDismiss={rewardSelection=null},onConfirm={ cents ->
                val error=change(if(selection.item!=null) "${selection.name}已永久获得" else "${selection.name}已送出，存款已记好") {
                    if(selection.item!=null) store.saveForItem(selection.item.id,cents) else store.saveForGift(selection.gift!!.id,cents)
                }
                if(error==null) {
                    rewardSelection=null; tab=0
                    react(SavingsWords.thanks(selection.name,selection.cents,reactionId),true)
                }
                error
            }) }
            entryDialog?.let { kind -> EntryDialog(kind,snapshot.savingsCents,onDismiss={entryDialog=null},onSubmit={ k,cents,category,note ->
                val error=change("这一笔记好了") {store.addEntry(k,cents,category,note)}
                if(error==null) {entryDialog=null; if(k==EntryKind.SAVING) react("你又认真存下了一笔，真棒！")}
                error
            }) }
            if(customGiftOpen) CustomGiftDialog(onDismiss={customGiftOpen=false},onSave={name,cents ->
                val error=change("自定义礼物已保存") {store.addCustomGift(name,cents)}
                if(error==null) customGiftOpen=false
                error
            })
            if(saveLookOpen) SaveLookDialog(onDismiss={saveLookOpen=false},onSave={name ->
                val error=change("当前穿搭已保存") {store.saveLook(name)}
                if(error==null) saveLookOpen=false
                error
            })
            if(settingsOpen) SettingsDialog(snapshot,store,onRestored={snapshot=store.snapshot();speech=null;celebrating=false;settingsOpen=false;scope.launch {snackbar.showSnackbar("备份已恢复")}},onDismiss={settingsOpen=false},onSubmit={name,goal ->
                val error=change("小计划已保存") {store.updateSettings(name,goal)}
                if(error==null) settingsOpen=false
                error
            })
            deletingEntry?.let {entry ->
                var error by remember(entry.id) {mutableStateOf<String?>(null)}
                AlertDialog(onDismissRequest={deletingEntry=null},title={Text("删除这一笔？")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("${entry.kind.displayName()} ¥${formatMoney(entry.cents)} 将从账本移除，余额会重新计算。")
                    if(entry.kind==EntryKind.SAVING || entry.kind==EntryKind.WITHDRAWAL) Text("已经获得的物品永久保留，赠礼纪念也会保留。",style=MaterialTheme.typography.bodySmall,color=Muted)
                    error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
                }},confirmButton={TextButton(onClick={error=change("已删除这一笔") {store.deleteEntry(entry.id)};if(error==null) deletingEntry=null}) {Text("删除这一笔",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(onClick={deletingEntry=null}) {Text("保留")}})
            }
        }
    }
}

@Composable
private fun HomePage(snapshot: CompanionSnapshot,celebrating: Boolean,speech: String?,onPetTap:()->Unit,onSave:()->Unit,onRecord:()->Unit,onItems:()->Unit,onGifts:()->Unit,modifier: Modifier=Modifier) {
    val progress=(snapshot.savingsCents.toDouble()/snapshot.goalCents.coerceAtLeast(1)).coerceIn(0.0,1.0).toFloat()
    LazyColumn(modifier,contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {Text("每天存一点，离目标近一点",style=MaterialTheme.typography.bodySmall,color=Muted);Spacer(Modifier.height(4.dp));Text("和${snapshot.companionName}一起\n把小存款攒成大目标",style=MaterialTheme.typography.headlineMedium)}
        item {SoftCard(padding=0) {
            RoomScene(snapshot.equippedItems,Modifier.fillMaxWidth(),celebrating,speech,onPetTap)
            Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {Text(snapshot.companionName,style=MaterialTheme.typography.titleMedium);Text(if(snapshot.ownedItemIds.isEmpty()) "从第一笔真实存款开始，一起慢慢积累" else "${snapshot.ownedItemIds.size} 件存款纪念 · ${snapshot.giftCount} 次赠礼存款",style=MaterialTheme.typography.bodySmall,color=Muted)}
                Text("点我聊存钱",style=MaterialTheme.typography.bodySmall,color=Muted)
            }
        }}
        item {Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Button(onClick=onItems,modifier=Modifier.weight(1f).heightIn(min=48.dp),shape=RoundedCornerShape(4.dp)) {Text("挑选物品")}
            OutlinedButton(onClick=onGifts,modifier=Modifier.weight(1f).heightIn(min=48.dp),shape=RoundedCornerShape(4.dp)) {Text("送份礼物")}
        }}
        item {SoftCard(color=Forest) {
            Text("真实存下的余额",color=Color(0xFFE5EBDD),style=MaterialTheme.typography.bodySmall)
            Text("¥ ${formatMoney(snapshot.savingsCents)}",color=Color.White,style=MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(12.dp));Text("目标 ¥${formatMoney(snapshot.goalCents)} · ${(progress*100).toInt()}%",color=Color(0xFFE5EBDD),style=MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp));LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(6.dp),color=Apricot,trackColor=Color(0xFF829382))
            Spacer(Modifier.height(10.dp));Text("物品和礼物对应的存款都计入这里",color=Color(0xFFE5EBDD),style=MaterialTheme.typography.bodySmall)
        }}
        item {Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick=onSave,modifier=Modifier.weight(1f),shape=RoundedCornerShape(4.dp)) {Text("单独存一笔")}
            OutlinedButton(onClick=onRecord,modifier=Modifier.weight(1f),shape=RoundedCornerShape(4.dp)) {Text("记一笔账")}
        }}
        item {SoftCard(color=PaleGreen) {
            Text("先选喜欢的，再存一笔",style=MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(5.dp));Text("例如选中 50 元的衣服，确认新存下 50 元后永久拥有。穿搭、发型、背景随时换；家具随时摆上或收起。",style=MaterialTheme.typography.bodyMedium)
        }}
        item {Text("App 记录你自己确认的真实存款，资金保留在你自己的账户。",style=MaterialTheme.typography.bodySmall,color=Muted)}
    }
}

@Composable
private fun ItemsPage(snapshot: CompanionSnapshot,onAcquire:(VillageItem)->Unit,onToggle:(VillageItem)->Unit,onHairColor:(String,String)->Unit,onSaveLook:()->Unit,onApplyLook:(Long)->Unit,modifier:Modifier=Modifier) {
    var category by rememberSaveable {mutableStateOf("全部")}
    var wardrobe by rememberSaveable {mutableStateOf(snapshot.ownedItemIds.isNotEmpty())}
    val items=ItemCatalog.items.filter {(category=="全部" || it.category.label==category) && ((it.id in snapshot.ownedItemIds)==wardrobe)}
    LazyColumn(modifier,contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {Text("每笔存款，都有纪念",style=MaterialTheme.typography.headlineMedium);Spacer(Modifier.height(5.dp));Text("一件物品，一笔新存款。获得后永久拥有，换装不增加存款。",color=Muted,style=MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {FilterChip(wardrobe,{wardrobe=true},label={Text("我的衣柜")});FilterChip(!wardrobe,{wardrobe=false},label={Text("待解锁")})}
        }
        if(wardrobe) item {SoftCard(padding=12) {
            RoomScene(snapshot.equippedItems,Modifier.fillMaxWidth())
            OutlinedButton(onClick=onSaveLook,modifier=Modifier.fillMaxWidth()) {Text("保存当前穿搭")}
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                snapshot.looks.forEach {look -> OutlinedButton(onClick={onApplyLook(look.id)}) {Text("换上：${look.name}")} }
            }
        }}
        item {Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            (listOf("全部")+ItemCategory.entries.map {it.label}).forEach {label -> FilterChip(selected=category==label,onClick={category=label},label={Text(label)})}
        };Text("${snapshot.ownedItemIds.size} / ${ItemCatalog.items.size} 件存款纪念",color=Muted,style=MaterialTheme.typography.bodySmall)}
        if(category=="发色") item {SoftCard {Text("基础黑色 · 随发型提供",style=MaterialTheme.typography.titleMedium);Text("每个发型新存一次 5 元永久解锁选色，之后自由切换。",color=Muted)}}
        if(items.isEmpty()) item {SoftCard {Text("这里还空空的",style=MaterialTheme.typography.titleMedium);Text("选择喜欢的物品，存一笔钱，把它带回家。",color=Muted,style=MaterialTheme.typography.bodyMedium)}}
        items(items,key={it.id}) {item ->
            val owned=item.id in snapshot.ownedItemIds
            val active=snapshot.isEquipped(item)
            val available=item.requiredHairId==null || item.requiredHairId in snapshot.ownedItemIds
            SoftCard(padding=14) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    ItemPixelPreview(item,Modifier.size(78.dp).clip(RoundedCornerShape(3.dp)),snapshot.equippedItems)
                    Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f)) {
                        Text(item.category.label,style=MaterialTheme.typography.bodySmall,color=Muted)
                        Text(item.name,style=MaterialTheme.typography.titleMedium);Text(item.subtitle,style=MaterialTheme.typography.bodySmall,color=Muted)
                        Spacer(Modifier.height(5.dp));Text(if(owned) "永久拥有 · ${if(active) "正在使用" else "已收藏"}" else "单次存款 ¥${formatMoney(item.priceCents)}",style=MaterialTheme.typography.bodySmall,color=Forest)
                    }
                }
                val hairId=if(item.category==ItemCategory.HAIR) item.id else item.requiredHairId
                if(owned && hairId!=null && "${hairId}_palette" in snapshot.ownedItemIds) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        listOf("black" to "黑色","brown" to "栗棕","pink" to "樱粉","silver" to "银色").forEach {(color,label) ->
                            val chosen=snapshot.equippedItems["HAIR_COLOR_$hairId"]?.substringAfterLast('_') ?: "black"
                            FilterChip(selected=chosen==color,onClick={onHairColor(hairId,color)},label={Text(label)})
                        }
                    }
                    if(item.category==ItemCategory.HAIR_COLOR) RoomScene(buildMap {putAll(snapshot.equippedItems);put(ItemCategory.HAIR.name,hairId)},Modifier.fillMaxWidth().padding(top=6.dp))
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick={if(owned) onToggle(item) else onAcquire(item)},enabled=available,modifier=Modifier.fillMaxWidth().heightIn(min=44.dp),shape=RoundedCornerShape(4.dp),colors=ButtonDefaults.buttonColors(containerColor=if(active) Color(0xFF91A28D) else Forest)) {
                    Text(if(!available) "请先获得对应发型" else if(!owned) "选择物品 · 存 ¥${formatMoney(item.priceCents)}" else if(active && item.category==ItemCategory.HAIR_COLOR) "恢复基础黑色" else if(active) "${if(item.category==ItemCategory.FURNITURE) "收起" else "换下"}这件物品" else "${if(item.category==ItemCategory.FURNITURE) "摆上" else "使用"}这件物品")
                }
            }
        }
    }
}

@Composable
private fun GiftsPage(snapshot:CompanionSnapshot,onGift:(Gift)->Unit,onCustomGift:()->Unit,modifier:Modifier=Modifier) {
    LazyColumn(modifier,contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {Text("存一笔，送一份纪念",style=MaterialTheme.typography.headlineMedium);Spacer(Modifier.height(6.dp));Text("每份礼物都记录一次真实存款，钱始终留在你的账户里。",color=Muted,style=MaterialTheme.typography.bodyMedium);OutlinedButton(onClick=onCustomGift,modifier=Modifier.fillMaxWidth()) {Text("创建自定义礼物")}}
        item {SoftCard(color=PaleGreen) {Text("已经送出 ${snapshot.giftCount} 份心意",style=MaterialTheme.typography.titleMedium);Spacer(Modifier.height(5.dp));Text("礼物可以重复赠送。每次都要确认一笔新的存款，小人会轻轻跳起感谢，并用聊天气泡回应你。",style=MaterialTheme.typography.bodyMedium)}}
        items(GiftCatalog.gifts+snapshot.customGifts,key={it.id}) {gift -> SoftCard {
            Row(verticalAlignment=Alignment.CenterVertically) {
                GiftPixelPreview(Modifier.size(88.dp),gift.id);Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {Text(gift.name,style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(5.dp));Text(gift.subtitle,color=Muted,style=MaterialTheme.typography.bodyMedium)}
            }
            Spacer(Modifier.height(14.dp));Text("每次新存 ¥${formatMoney(gift.priceCents)} · 可反复赠送",style=MaterialTheme.typography.bodySmall,color=Muted)
            Spacer(Modifier.height(8.dp));Button(onClick={onGift(gift)},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(4.dp)) {Text("存 ¥${formatMoney(gift.priceCents)} · 送给${snapshot.companionName}")}
        }}
    }
}

@Composable
private fun RewardDepositDialog(selection:RewardSelection,onDismiss:()->Unit,onConfirm:(Long)->String?) {
    var confirmed by remember(selection) {mutableStateOf(false)}
    var error by remember(selection) {mutableStateOf<String?>(null)}
    FormDialog(title=if(selection.item!=null) "为物品存一笔" else "为礼物存一笔",onDismiss=onDismiss) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            selection.item?.let {ItemPixelPreview(it,Modifier.size(72.dp))} ?: GiftPixelPreview(Modifier.size(72.dp),selection.gift!!.id)
            Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)) {Text(selection.name,style=MaterialTheme.typography.titleLarge);Text("本次存款 ¥${formatMoney(selection.cents)}",style=MaterialTheme.typography.titleMedium)}
        }
        Text(if(selection.item!=null) "这笔存款会记入账本，物品永久获得并立即使用。以后更换装扮不需要再次存钱。" else "这笔存款会记入账本，并赠送一份礼物。每次再次赠送，都需要另存一笔。",color=Muted,style=MaterialTheme.typography.bodyMedium)
        Text("请先在自己的账户存好对应金额，再确认。已记录的旧存款不用于本次兑换。",color=Muted,style=MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().clickable {confirmed=!confirmed;error=null},verticalAlignment=Alignment.CenterVertically) {
            Checkbox(confirmed,onCheckedChange={confirmed=it;error=null})
            Text("我已新存下 ¥${formatMoney(selection.cents)}，这笔钱尚未记入账本",modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
        }
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        Button(onClick={error=if(!confirmed) "请先确认已经实际存下这笔新的存款" else onConfirm(selection.cents)},modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),shape=RoundedCornerShape(4.dp)) {Text(if(selection.item!=null) "确认存款 · 永久获得" else "确认存款 · 赠送礼物")}
    }
}
@Composable
private fun LedgerPage(
    snapshot: CompanionSnapshot,
    onRecord: () -> Unit,
    onWithdraw: () -> Unit,
    onDelete: (MoneyEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by rememberSaveable { mutableStateOf("全部") }
    val monthStart = remember(snapshot.entries) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val monthEnd = remember(monthStart) { Calendar.getInstance().apply { timeInMillis = monthStart; add(Calendar.MONTH, 1) }.timeInMillis }
    val monthly = snapshot.entries.filter { it.createdAt >= monthStart && it.createdAt < monthEnd }
    val income = monthly.filter { it.kind == EntryKind.INCOME }.sumOf { it.cents }
    val expense = monthly.filter { it.kind == EntryKind.EXPENSE }.sumOf { it.cents }
    val entries = snapshot.entries.filter {
        when (filter) {
            "收入" -> it.kind == EntryKind.INCOME
            "支出" -> it.kind == EntryKind.EXPENSE
            "储蓄" -> it.kind == EntryKind.SAVING || it.kind == EntryKind.WITHDRAWAL
            else -> true
        }
    }.sortedByDescending { it.createdAt }
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("认真记下生活", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(5.dp))
            Text("收支有数，攒钱也有自己的节奏。", color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        item {
            SoftCard {
                Text(SimpleDateFormat("yyyy 年 M 月", Locale.CHINA).format(Date()), color = Muted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("本月收入", color = Muted, style = MaterialTheme.typography.bodySmall)
                        Text("¥${formatMoney(income)}", style = MaterialTheme.typography.titleLarge)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("本月支出", color = Muted, style = MaterialTheme.typography.bodySmall)
                        Text("¥${formatMoney(expense)}", style = MaterialTheme.typography.titleLarge)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Surface(color = PaleGreen, shape = RoundedCornerShape(4.dp)) {
                    Row(Modifier.fillMaxWidth().padding(start = 13.dp, end = 4.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("储蓄余额", style = MaterialTheme.typography.bodySmall, color = Muted)
                            Text("¥${formatMoney(snapshot.savingsCents)}", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = onWithdraw) { Text("取出储蓄") }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("存入和取出储蓄单独记录，不计入收入、支出。", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
        item {
            Button(onClick = onRecord, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(4.dp)) { Text("＋ 记一笔账") }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                listOf("全部", "支出", "收入", "储蓄").forEach { choice ->
                    FilterChip(selected = filter == choice, onClick = { filter = choice }, label = { Text(choice) })
                }
            }
        }
        if (entries.isEmpty()) {
            item {
                SoftCard {
                    Text("还没有${if (filter == "全部") "" else filter}记录", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(7.dp))
                    Text("从今天的一笔小记录开始，慢慢看见自己的生活。", color = Muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(entries, key = { it.id }) { entry ->
                SoftCard(padding = 16) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(38.dp).background(if (entry.kind == EntryKind.SAVING) PaleGreen else Color(0xFFF2EBDF), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                            Text(when (entry.kind) { EntryKind.SAVING -> "存"; EntryKind.WITHDRAWAL -> "取"; EntryKind.INCOME -> "收"; EntryKind.EXPENSE -> "支" }, style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(entry.category, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Spacer(Modifier.width(8.dp))
                                Text("${if (entry.kind == EntryKind.INCOME || entry.kind == EntryKind.SAVING) "+" else "−"}¥${formatMoney(entry.cents)}", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            }
                            if (entry.note.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(entry.note, style = MaterialTheme.typography.bodyMedium, color = Muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${entry.kind.displayName()} · ${SimpleDateFormat("M/d HH:mm", Locale.CHINA).format(Date(entry.createdAt))}", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.weight(1f))
                                TextButton(onClick = { onDelete(entry) }, contentPadding = PaddingValues(horizontal = 5.dp)) { Text("删除", style = MaterialTheme.typography.bodySmall, color = Muted) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryDialog(
    initialKind: EntryKind,
    savingsCents: Long,
    onDismiss: () -> Unit,
    onSubmit: (EntryKind, Long, String, String) -> String?,
) {
    var kind by remember(initialKind) { mutableStateOf(initialKind) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("餐饮") }
    var note by remember { mutableStateOf("") }
    var actualSavingsConfirmed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val saving = kind == EntryKind.SAVING
    val withdrawal = kind == EntryKind.WITHDRAWAL
    val categories = if (kind == EntryKind.INCOME) listOf("工资", "奖金", "兼职", "其他") else listOf("餐饮", "购物", "交通", "居住", "娱乐", "其他")
    FormDialog(title = when { saving -> "存下一笔"; withdrawal -> "取出储蓄"; else -> "记一笔账" }, onDismiss = onDismiss) {
        if (!saving && !withdrawal) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(EntryKind.EXPENSE, EntryKind.INCOME).forEach { option ->
                    FilterChip(selected = kind == option, onClick = {
                        kind = option
                        category = if (option == EntryKind.INCOME) "工资" else "餐饮"
                        error = null
                    }, label = { Text(option.displayName()) })
                }
            }
        }
        if (saving) Text("把已经实际存下的钱记在这里。想获得物品或赠送礼物，请从对应页面选择后确认存款。", style = MaterialTheme.typography.bodyMedium, color = Muted)
        if (withdrawal) Text("可取出 ¥${formatMoney(savingsCents)}。这里仅更新记录，请自行完成现实中的资金操作。", style = MaterialTheme.typography.bodyMedium, color = Muted)
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it; error = null },
            label = { Text("金额（元）") },
            placeholder = { Text("0.00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
        )
        if (!saving && !withdrawal) {
            Text("分类", style = MaterialTheme.typography.bodySmall, color = Muted)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { option -> FilterChip(selected = category == option, onClick = { category = option }, label = { Text(option) }) }
            }
        }
        OutlinedTextField(
            value = note,
            onValueChange = { if (it.length <= 100) note = it },
            label = { Text("备注（选填）") },
            placeholder = { Text(if (saving) "例如：今天少买了一杯奶茶" else "记下这一笔的小细节") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            shape = RoundedCornerShape(4.dp),
        )
        if (saving) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { actualSavingsConfirmed = !actualSavingsConfirmed; error = null },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = actualSavingsConfirmed, onCheckedChange = { actualSavingsConfirmed = it; error = null })
                Text("这笔钱已实际存入我的储蓄", modifier = Modifier.weight(1f).padding(end = 5.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        Button(
            onClick = {
                val cents = parseMoney(amount)
                error = when {
                    cents == null -> "请输入大于 0 的有效金额，最多保留两位小数"
                    saving && !actualSavingsConfirmed -> "请先确认这笔钱已经实际存入储蓄"
                    withdrawal && cents > savingsCents -> "取出金额不能超过当前储蓄余额"
                    else -> onSubmit(kind, cents, when { saving -> "存入储蓄"; withdrawal -> "取出储蓄"; else -> category }, note.trim())
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            shape = RoundedCornerShape(4.dp),
        ) { Text(when { saving -> "确认记录储蓄"; withdrawal -> "确认记录取出"; else -> "记好了" }) }
    }
}

@Composable
private fun SettingsDialog(snapshot: CompanionSnapshot,store: CompanionStore,onRestored:()->Unit, onDismiss: () -> Unit, onSubmit: (String, Long) -> String?) {
    var name by remember { mutableStateOf(snapshot.companionName) }
    var goal by remember { mutableStateOf(formatMoney(snapshot.goalCents)) }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(title = "我们的小计划", onDismiss = onDismiss) {
        Text("给小人起个名字，再定一个舒服的小目标。", color = Muted, style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(value = name, onValueChange = { name = it; error = null }, label = { Text("小人的名字") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp))
        OutlinedTextField(value = goal, onValueChange = { goal = it; error = null }, label = { Text("储蓄目标（元）") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp))
        Text("目标随时可以调整，不用着急。", color = Muted, style = MaterialTheme.typography.bodySmall)
        Text("数据仅保存在这台手机。卸载应用或清除数据会丢失记录。", color = Muted, style = MaterialTheme.typography.bodySmall)
        BackupControls(store,onRestored)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        Button(onClick = {
            val goalCents = parseMoney(goal)
            error = when {
                name.trim().isEmpty() -> "请给小人起一个名字"
                name.trim().length > 12 -> "名字请控制在 12 个字以内"
                goalCents == null -> "请输入大于 0 的目标金额，最多保留两位小数"
                else -> onSubmit(name.trim(), goalCents)
            }
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(4.dp)) { Text("保存小计划") }
    }
}

@Composable
internal fun FormDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().imePadding().padding(18.dp), contentAlignment = Alignment.Center) {
            Surface(modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(), shape = RoundedCornerShape(6.dp), color = Paper) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("取消") }
                    }
                    content()
                }
            }
        }
    }
}

@Composable
private fun SoftCard(color: Color = Paper, padding: Int = 20, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = color, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(padding.dp), content = content)
    }
}

private fun EntryKind.displayName(): String = when (this) {
    EntryKind.EXPENSE -> "支出"
    EntryKind.INCOME -> "收入"
    EntryKind.SAVING -> "存入"
    EntryKind.WITHDRAWAL -> "取出"
}
