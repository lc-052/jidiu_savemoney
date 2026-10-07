package com.jidiu.companion

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun RoomScene(
    equippedItems: Map<String, String>, modifier: Modifier = Modifier,
    celebrating: Boolean = false, bubbleText: String? = null, onPetTap: () -> Unit = {},
) {
    val context=LocalContext.current
    val art=remember(context.applicationContext) {CharacterArt.get(context.applicationContext)}
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(celebrating) {
        frame = 0
        while (true) { delay(if (celebrating) 180 else 350); frame = (frame + 1) % 24 }
    }
    val picture = remember(equippedItems, frame, celebrating,art) { renderRoom(equippedItems, frame, celebrating,art) }
    Box(modifier.aspectRatio(144f / 112f).background(Color(0xFFEFE7DC)).semantics {
        contentDescription = if (celebrating) "像素小人开心地轻跳感谢，周围飘着爱心" else "Q版像素小人与房间，轻触可以打招呼"
    }) {
        Canvas(Modifier.fillMaxSize()) {
            drawImage(picture, dstSize = IntSize(size.width.toInt(), size.height.toInt()), filterQuality = FilterQuality.None)
        }
        Box(Modifier.align(Alignment.Center).offset(y = 28.dp).size(115.dp, 145.dp).clickable(onClickLabel = "和小人打招呼", onClick = onPetTap))
        if (bubbleText != null) {
            Column(Modifier.align(Alignment.TopCenter).padding(top = 10.dp).widthIn(max = 260.dp).padding(horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(bubbleText, color = Color(0xFF594A40), fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.background(Color(0xFFFFFEF6), RoundedCornerShape(3.dp)).border(2.dp, Color(0xFFA58C70), RoundedCornerShape(3.dp)).padding(horizontal = 12.dp, vertical = 8.dp))
                Canvas(Modifier.size(12.dp, 7.dp)) {
                    drawRect(Color(0xFFA58C70), size = androidx.compose.ui.geometry.Size(size.width, size.height / 2))
                    drawRect(Color(0xFFA58C70), topLeft = androidx.compose.ui.geometry.Offset(size.width / 3, size.height / 2), size = androidx.compose.ui.geometry.Size(size.width / 3, size.height / 2))
                }
            }
        }
    }
}

@Composable
fun ItemPixelPreview(item: VillageItem, modifier: Modifier = Modifier, equipment: Map<String,String> = emptyMap()) {
    val context=LocalContext.current
    val art=remember(context.applicationContext) {CharacterArt.get(context.applicationContext)}
    val picture = remember(item.id,art,equipment) { renderPreview(item,art,equipment) }
    Canvas(modifier.semantics { contentDescription = "${item.name}的像素预览" }) {
        drawImage(picture, dstSize = IntSize(size.width.toInt(), size.height.toInt()), filterQuality = FilterQuality.None)
    }
}
@Composable
fun GiftPixelPreview(modifier: Modifier = Modifier,giftId: String = "heartbox") {
    val picture = remember(giftId) {
        pixelImage(48, 48) { p ->
            p.rect(0, 0, 48, 48, "F6F0E5")
            when(giftId) {
                "flower" -> {
                    p.rect(23,20,2,20,"628975");p.rect(16,28,8,4,"8FB795");p.rect(25,32,7,4,"8FB795")
                    p.rect(19,8,10,20,"F1A9C0");p.rect(14,13,20,10,"F1A9C0");p.rect(21,15,6,6,"FFE1A0")
                }
                "cake" -> {
                    p.rect(9,37,30,3,"A57B63");p.rect(12,23,24,14,"D995A8");p.rect(12,21,24,6,"FFFDF8")
                    p.rect(12,30,24,3,"F5D1C9");p.rect(22,12,3,9,"91A7D0");p.rect(22,7,3,4,"FFE1A0")
                }
                "bookgift" -> {
                    p.rect(13,9,25,31,"779D9A");p.rect(10,10,5,30,"556957");p.rect(16,36,21,3,"FFFDF8")
                    p.rect(18,15,14,2,"E7ECE4");p.rect(18,20,11,2,"E7ECE4");p.star(21,26)
                }
                "starjar" -> {
                    p.rect(13,12,22,5,"A57B63");p.rect(10,17,28,19,"AFCADD");p.rect(13,36,22,4,"AFCADD")
                    p.rect(13,19,22,16,"E0EDF0");p.star(20,24);p.rect(18,12,12,2,"556957")
                }
                "trophy" -> {
                    p.rect(9,12,30,3,"C79645");p.rect(9,15,4,11,"C79645");p.rect(35,15,4,11,"C79645")
                    p.rect(13,10,22,16,"FFE1A0");p.rect(17,26,14,4,"EDBF63");p.rect(22,30,4,7,"C79645")
                    p.rect(16,37,16,4,"A57B63");p.star(20,16)
                }
                else -> p.gift(24,37)
            }
        }
    }
    Canvas(modifier.semantics { contentDescription = "粉色心意礼盒" }) {
        drawImage(picture, dstSize = IntSize(size.width.toInt(), size.height.toInt()), filterQuality = FilterQuality.None)
    }
}

private fun pixelImage(w: Int, h: Int, draw: (PixelPainter) -> Unit): ImageBitmap {
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    draw(PixelPainter(AndroidCanvas(bitmap)))
    return bitmap.asImageBitmap()
}
private class PixelPainter(val canvas: AndroidCanvas) {
    private val paint = Paint().apply { isAntiAlias = false; isFilterBitmap = false }
    fun rect(x: Int, y: Int, w: Int, h: Int, color: String) {
        paint.color = android.graphics.Color.parseColor("#$color")
        canvas.drawRect(x.toFloat(), y.toFloat(), (x+w).toFloat(), (y+h).toFloat(), paint)
    }
    fun shape(color: String, vararg xy: Int) {
        require(xy.size>=6 && xy.size%2==0)
        val path=Path().apply {moveTo(xy[0].toFloat(),xy[1].toFloat());for(i in 2 until xy.size step 2) lineTo(xy[i].toFloat(),xy[i+1].toFloat());close()}
        paint.color=android.graphics.Color.parseColor("#$color")
        canvas.drawPath(path,paint)
    }
    fun block(x: Int, y: Int, rows: String, palette: Map<Char, String>, scale: Int = 1) {
        rows.trimIndent().lines().forEachIndexed { j, row -> row.forEachIndexed { i, c -> palette[c]?.let { rect(x+i*scale, y+j*scale, scale, scale, it) } } }
    }
    fun heart(x: Int, y: Int, color: String = "E69DAF") = block(x, y, ".XX.XX.\nXXXXXXX\nXXXXXXX\n.XXXXX.\n..XXX..\n...X...", mapOf('X' to color))
    fun star(x: Int, y: Int) = block(x, y, "...X...\n...X...\n.XXXXX.\nXXXXXXX\n..XXX..\n.XX.XX.", mapOf('X' to "FFE1A0"))
    fun gift(x: Int, y: Int) {
        rect(x-13, y-22, 26, 23, "78608C"); rect(x-11, y-20, 22, 19, "EDA8BE")
        rect(x-14, y-22, 28, 7, "B978A3"); rect(x-12, y-21, 24, 4, "F7C0D0")
        rect(x-2, y-22, 4, 22, "FFE4AF"); rect(x-2, y-22, 2, 22, "FFF3CF")
        block(x-10, y-30, ".XXX...XXX.\nXX.XX.XX.XX\nXX..XXX..XX\n.XXXXXXXXX.\n...XXXXX...", mapOf('X' to "D787AA"), 1)
        heart(x-3, y-12, "FFF4E3")
    }
    fun background(id: String?) {
        rect(0, 0, 144, 112, "D9CFC0"); rect(3, 3, 138, 78, "FFF9EE")
        when (id) {
            "sakura" -> {
                rect(3, 3, 138, 78, "F9DFEC"); rect(3, 3, 138, 32, "DAC9EE")
                rect(17, 24, 25, 5, "FFF6F3"); rect(22, 20, 15, 5, "FFF6F3")
                rect(123, 32, 4, 47, "B388A0"); rect(109, 37, 17, 3, "B388A0")
                rect(106, 20, 35, 23, "EAA7C4"); rect(100, 26, 41, 12, "F1B9D2"); rect(114, 17, 20, 5, "F6C5D9")
                listOf(15 to 55, 35 to 42, 97 to 64, 133 to 54).forEach { (x,y) -> rect(x,y,2,2,"E9A1C2") }
            }
            "forest" -> {
                rect(3, 3, 138, 78, "DCEDE3"); rect(3, 3, 138, 35, "C3DCE9")
                rect(23, 15, 10, 10, "FFF0BF")
                for (x in listOf(7, 27, 99, 120)) {
                    rect(x+7, 39, 3, 39, "9E9A7C"); rect(x+3, 27, 12, 29, "97B8A1"); rect(x, 35, 19, 15, "B5CDB0"); rect(x+5, 22, 8, 8, "ACC7B3")
                }
                rect(3, 72, 138, 9, "BDD1AC")
            }
            "night" -> {
                rect(3, 3, 138, 78, "615E8F"); rect(3, 53, 138, 28, "77719E")
                rect(109, 15, 12, 14, "FFE4AF"); rect(114, 12, 9, 5, "FFE4AF"); rect(116, 13, 8, 12, "615E8F")
                listOf(16 to 16, 48 to 9, 83 to 27, 25 to 45, 129 to 46, 61 to 36).forEach { (x,y) -> star(x,y) }
            }
        }
        rect(3, 81, 138, 28, if (id == "night") "B8AECF" else "E4CFB4")
        rect(3, 79, 138, 2, "CBB69A")
        for (x in 16..140 step 22) rect(x, 84, 1, 25, "D6BFA0")
        rect(3, 94, 138, 1, "D6BFA0")
    }
    fun furniture(id: String, x: Int, y: Int) {
        when(id) {
            "plant" -> {
                rect(x-5,y-11,10,11,"A17491"); rect(x-6,y-12,12,3,"C894AD"); rect(x-3,y-8,2,7,"D3A7BB")
                rect(x,y-26,2,14,"628975"); rect(x-6,y-22,7,5,"8FB795"); rect(x+1,y-28,7,5,"ADD1A1"); rect(x-3,y-32,5,7,"709B83")
            }
            "book" -> {
                rect(x-12,y-35,24,35,"96738B"); rect(x-10,y-33,20,30,"D0A3A4")
                for (yy in listOf(y-22,y-8)) { rect(x-10,yy,20,2,"8B697F"); rect(x-8,yy-10,4,10,"9CBFC3"); rect(x-3,yy-8,4,8,"E7BCC1"); rect(x+3,yy-11,5,11,"EAD3A3") }
                rect(x-13,y-36,26,3,"BB8DA0"); rect(x-10,y-3,3,3,"795E7B"); rect(x+7,y-3,3,3,"795E7B")
            }
            "lamp" -> {
                rect(x-5,y-2,10,2,"8E7190"); rect(x,y-32,2,31,"AE879C"); rect(x-6,y-34,13,3,"EACCAA")
                rect(x-5,y-31,11,4,"FBE3B1"); rect(x-7,y-27,15,4,"FFECC5"); rect(x-8,y-23,17,3,"B58DA0")
                rect(x-5,y-20,12,5,"E9CFB3")
            }
            "rug" -> { rect(x-22,y-9,44,9,"B398BD"); rect(x-24,y-7,48,5,"C9B6D6"); rect(x-19,y-7,38,5,"EEE5F3"); heart(x-3,y-6,"D8B4D4") }
            "star" -> { rect(x,y-20,1,13,"A887AB"); star(x-3,y-7) }
            "bed" -> {
                rect(x-19,y-22,39,22,"987B96"); rect(x-17,y-19,35,17,"E6B0C7"); rect(x-15,y-18,13,6,"FFF0ED")
                rect(x-2,y-18,17,6,"F8D6DC"); rect(x-17,y-10,35,8,"CB96B6"); heart(x+7,y-9,"F3C7D6")
                rect(x-19,y-2,4,4,"785D7C"); rect(x+16,y-2,4,4,"785D7C")
            }
            "desk" -> {
                rect(x-15,y-20,31,4,"C39BAD"); rect(x-13,y-16,3,16,"997A93"); rect(x+11,y-16,3,16,"997A93")
                rect(x+2,y-16,9,8,"DCB2BC"); rect(x+5,y-13,2,1,"92728D"); rect(x-9,y-23,10,3,"FFF0D3")
            }
            "window" -> {
                rect(x-16,y-36,32,34,"A68AAE"); rect(x-14,y-34,28,30,"B6D8E9"); rect(x-12,y-29,8,3,"F8FAED")
                rect(x+7,y-31,4,4,"FFF0B8"); rect(x-1,y-34,2,30,"F8EDF6"); rect(x-14,y-20,28,2,"F8EDF6")
                rect(x-18,y-4,36,4,"CBAFBF")
            }
        }
    }

}
private fun renderRoom(equipment: Map<String,String>, frame: Int, celebrating: Boolean,art: CharacterArt): ImageBitmap = pixelImage(864,672) { p ->
    p.canvas.scale(6f,6f)
    p.background(equipment[ItemCategory.BACKGROUND.name])
    val active = equipment.values.toSet()
    if("window" in active) p.furniture("window",24,48)
    if("book" in active) p.furniture("book",120,79)
    if("desk" in active) p.furniture("desk",23,76)
    if("bed" in active) p.furniture("bed",26,104)
    if("lamp" in active) p.furniture("lamp",100,98)
    if("plant" in active) p.furniture("plant",126,105)
    if("star" in active) p.furniture("star",86,28)
    if("rug" in active) p.furniture("rug",72,103)
    p.rect(61,98,23,2,"CAB393")
    art.draw(p.canvas,equipment,frame,celebrating)
    if(celebrating) {
        val dy = frame % 8
        p.heart(49,46-dy*2); p.heart(92,40-dy*2,"D9A9D9"); p.star(97,69-dy)
    }
}
internal fun previewEquipment(item: VillageItem,equipment: Map<String,String>): Map<String,String> = buildMap {
    putAll(equipment)
    if(item.category==ItemCategory.HAIR_COLOR) {
        val hairId=item.requiredHairId!!
        put(ItemCategory.HAIR.name,hairId)
        // A missing saved selection means basic black, including newly unlocked palettes.
    } else put(item.slot,item.id)
}
internal fun renderPreview(item: VillageItem,art: CharacterArt,equipment: Map<String,String> = emptyMap()): ImageBitmap = pixelImage(256,256) { p ->
    p.canvas.scale(4f,4f)
    p.rect(0,0,64,64,"F6F0E5")
    when(item.category) {
        ItemCategory.BACKGROUND -> { p.canvas.save(); p.canvas.scale(64f/144,64f/112); p.background(item.id); p.canvas.restore() }
        ItemCategory.FURNITURE -> p.furniture(item.id,32,54)
        else -> { p.canvas.save(); p.canvas.translate(-40f,-35f); p.canvas.scale(0.88f,0.88f,72f,98f); art.draw(p.canvas,previewEquipment(item,equipment),0,false); p.canvas.restore() }
    }
}
