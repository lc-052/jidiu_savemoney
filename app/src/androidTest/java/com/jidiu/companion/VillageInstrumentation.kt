package com.jidiu.companion

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle

/** Checks the actual Android database, with isolated database names. */
class VillageInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val completed = mutableListOf<String>()
        fun pass(name: String) { completed += name; sendStatus(0, Bundle().apply { putString("stream", "PASS: $name\n") }) }
        try {
            val name = "village-test-${System.nanoTime()}.db"
            var store = CompanionStore(targetContext, name)
            check(store.snapshot().ownedItemIds.isEmpty() && store.snapshot().equippedItems.isEmpty())
            check(store.snapshot().companionName == "寄丢")
            pass("new installation has an empty room and blank character")
            store.addEntry(EntryKind.SAVING, 10000, "普通存款", "")
            check(store.snapshot().ownedItemIds.isEmpty())
            store.saveForItem("hoodie", 5000)
            check(store.snapshot().savingsCents == 15000L && store.snapshot().entries.size == 2)
            check(store.snapshot().isEquipped(ItemCatalog.items.first { it.id == "hoodie" }))
            pass("old savings do not acquire items; choosing an item adds exactly one new deposit")
            check(runCatching { store.saveForItem("hoodie", 5000) }.isFailure)
            check(store.snapshot().entries.size == 2 && store.snapshot().savingsCents == 15000L)
            check(runCatching { store.saveForItem("sailor", 1) }.isFailure)
            check(store.snapshot().entries.size == 2 && "sailor" !in store.snapshot().ownedItemIds)
            pass("duplicate and wrong-price acquisitions roll back without adding money")
            store.saveForGift("heartbox", 1000); store.saveForGift("heartbox", 1000)
            check(store.snapshot().giftCount == 2 && store.snapshot().savingsCents == 17000L)
            check(store.snapshot().entries.count { it.category == "礼物存款" } == 2)
            pass("repeat gifting creates two independent saving entries and two gift events")
            store.saveForItem("bob", 3000); store.saveForItem("pinkhair", 6000)
            check(store.snapshot().equippedItems[ItemCategory.HAIR.name] == "pinkhair")
            check(setOf("bob", "pinkhair").all { it in store.snapshot().ownedItemIds })
            store.toggleItem("hoodie")
            check("hoodie" in store.snapshot().ownedItemIds && ItemCategory.CLOTHES.name !in store.snapshot().equippedItems)
            store.toggleItem("hoodie")
            pass("switching and removing equipment preserves permanent ownership")
            store.addEntry(EntryKind.WITHDRAWAL, 23000, "取出", "")
            check(store.snapshot().savingsCents == 3000L && store.snapshot().ownedItemIds.size == 3)
            val itemEntry = store.snapshot().entries.first { it.note == "永久获得：蜜桃连帽衫" }
            check(runCatching { store.deleteEntry(itemEntry.id) }.isFailure)
            check(store.snapshot().savingsCents == 3000L)
            pass("withdrawal preserves items and overdrawing corrections are rejected")
            store.close(); store = CompanionStore(targetContext, name)
            check(store.snapshot().giftCount == 2 && store.snapshot().ownedItemIds.size == 3)
            check(store.snapshot().equippedItems[ItemCategory.HAIR.name] == "pinkhair")
            pass("balances, ownership, equipment and gift events survive reopening")
            val withdrawal = store.snapshot().entries.first { it.kind == EntryKind.WITHDRAWAL }
            store.deleteEntry(withdrawal.id); store.deleteEntry(itemEntry.id)
            check("hoodie" in store.snapshot().ownedItemIds)
            pass("correcting an acquisition deposit preserves the permanent item")
            val beforeNameUpgrade = store.snapshot().savingsCents
            store.updateSettings("小满", 50000)
            store.close()
            targetContext.openOrCreateDatabase(name, 0, null).use { it.version = 2 }
            CompanionStore(targetContext, name).use { renamed ->
                check(renamed.snapshot().companionName == "寄丢")
                check(renamed.snapshot().savingsCents == beforeNameUpgrade && renamed.snapshot().ownedItemIds.size == 5)
                check(setOf("bob_palette","pinkhair_palette").all {it in renamed.snapshot().ownedItemIds})
                check(renamed.snapshot().equippedItems["HAIR_COLOR_pinkhair"]=="pinkhair_pink")
            }
            pass("v2 upgrade changes the old default name to Jidiu and keeps savings and items")

            val legacyName = "village-upgrade-${System.nanoTime()}.db"
            targetContext.openOrCreateDatabase(legacyName, 0, null).use { db ->
                db.execSQL("CREATE TABLE entries (id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, cents INTEGER NOT NULL, category TEXT NOT NULL, note TEXT NOT NULL, created_at INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE gifts (id TEXT PRIMARY KEY, delivered_at INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE settings (id INTEGER PRIMARY KEY, name TEXT NOT NULL, goal_cents INTEGER NOT NULL)")
                db.execSQL("INSERT INTO entries VALUES (1, 'SAVING', 5000, '旧存款', '', 1)")
                db.execSQL("INSERT INTO gifts VALUES ('plant', 1)")
                db.execSQL("INSERT INTO settings VALUES (1, '旧伙伴', 50000)")
                db.version = 1
            }
            CompanionStore(targetContext, legacyName).use { upgraded ->
                val state = upgraded.snapshot()
                check(state.savingsCents == 5000L && state.entries.size == 1 && state.companionName == "旧伙伴")
                check("plant" in state.ownedItemIds && state.equippedItems["plant"] == "plant")
                upgraded.saveForGift("heartbox", 1000)
                check(upgraded.snapshot().savingsCents == 6000L && upgraded.snapshot().giftCount == 1)
            }
            pass("v1 upgrade preserves the ledger, settings and old furniture without duplicate deposits")
            val appearanceName="village-appearance-${System.nanoTime()}.db"
            CompanionStore(targetContext,appearanceName).use {appearance ->
                check(runCatching {appearance.saveForItem("bob_palette",500)}.isFailure)
                check(appearance.snapshot().entries.isEmpty())
                appearance.saveForItem("bob",3000)
                check("HAIR_COLOR_bob" !in appearance.snapshot().equippedItems)
                check(runCatching {appearance.selectHairColor("bob","pink")}.isFailure)
                appearance.selectHairColor("bob","black")
                check(appearance.snapshot().entries.size==1)
                pass("new hairstyles start black; paid color selection cannot be bypassed")
                appearance.saveForItem("bob_palette",500)
                val paid=appearance.snapshot()
                check(paid.savingsCents==3500L && paid.entries.size==2)
                appearance.selectHairColor("bob","pink");appearance.selectHairColor("bob","silver");appearance.selectHairColor("bob","black")
                check(appearance.snapshot().entries.size==2 && appearance.snapshot().savingsCents==3500L)
                check("bob_palette" in appearance.snapshot().ownedItemIds && "HAIR_COLOR_bob" !in appearance.snapshot().equippedItems)
                check(runCatching {appearance.saveForItem("bob_palette",500)}.isFailure)
                check(runCatching {appearance.selectHairColor("bob","unknown")}.isFailure)
                pass("one five-yuan deposit permanently unlocks free color changes for that hairstyle")
                appearance.saveForItem("pinkhair",6000)
                check(runCatching {appearance.selectHairColor("pinkhair","pink")}.isFailure)
                appearance.selectHairColor("bob","brown")
                appearance.toggleItem("bob")
                appearance.toggleItem("bob_palette")
                check(appearance.snapshot().equippedItems["HAIR"]=="bob" && appearance.snapshot().equippedItems["HAIR_COLOR_bob"]=="bob_brown")
                check(appearance.snapshot().equippedItems["HAIR_COLOR_bob"]=="bob_brown")
                check(appearance.snapshot().savingsCents==9500L && appearance.snapshot().entries.size==3)
            }
            CompanionStore(targetContext,appearanceName).use {appearance ->
                check(appearance.snapshot().equippedItems["HAIR_COLOR_bob"]=="bob_brown")
                appearance.selectHairColor("bob","silver")
                check(appearance.snapshot().savingsCents==9500L && appearance.snapshot().entries.size==3)
            }
            pass("free unlocked hair colors persist after reopening")
            val skinLegacyName="retired-skin-${System.nanoTime()}.db"
            CompanionStore(targetContext,skinLegacyName).use {legacy -> legacy.addEntry(EntryKind.SAVING,1000,"物品存款","永久获得：浅肤色") }
            targetContext.openOrCreateDatabase(skinLegacyName,0,null).use {db ->
                db.execSQL("INSERT INTO owned_items(id,acquired_at,entry_id) VALUES ('skin_ivory',1,1)")
                db.execSQL("INSERT INTO equipment VALUES ('SKIN','skin_ivory')")
                db.version=4
            }
            CompanionStore(targetContext,skinLegacyName).use {upgraded ->
                check(upgraded.snapshot().savingsCents==1000L && upgraded.snapshot().entries.size==1)
                check("SKIN" !in upgraded.snapshot().equippedItems && "skin_ivory" !in upgraded.snapshot().ownedItemIds)
                check(runCatching {upgraded.saveForItem("skin_ivory",1000)}.isFailure)
            }
            pass("retired skin customization preserves all historical deposits on upgrade")
            val expandedName="expanded-items-${System.nanoTime()}.db"
            CompanionStore(targetContext,expandedName).use {expanded ->
                for(id in listOf("bun","ponytail","braid","wave")) {
                    expanded.saveForItem(id,ItemCatalog.items.first {it.id==id}.priceCents)
                    check(expanded.snapshot().equippedItems["HAIR"]==id)
                    check(expanded.snapshot().equippedItems["HAIR_COLOR_$id"]==null)
                    check(runCatching {expanded.selectHairColor(id,"pink")}.isFailure)
                    expanded.saveForItem("${id}_palette",500)
                    expanded.selectHairColor(id,"pink")
                }
                for(id in listOf("cardigan","overalls","mintdress","yukata")) expanded.saveForItem(id,ItemCatalog.items.first {it.id==id}.priceCents)
                check(expanded.snapshot().savingsCents==58000L && expanded.snapshot().entries.size==12)
                expanded.selectHairColor("wave","silver")
                expanded.toggleItem("bun")
                check(expanded.snapshot().equippedItems["HAIR_COLOR_bun"]=="bun_pink")
                check(expanded.snapshot().savingsCents==58000L && expanded.snapshot().entries.size==12)
            }
            pass("all new hairstyles, individual palettes and outfits require exactly one matching deposit")
            CompanionStore(targetContext,expandedName).use {expanded ->
                check(expanded.snapshot().ownedItemIds.size==12)
                check(expanded.snapshot().equippedItems["HAIR"]=="bun")
                check(expanded.snapshot().equippedItems["CLOTHES"]=="yukata")
                expanded.selectHairColor("ponytail","brown")
                check(expanded.snapshot().equippedItems["HAIR"]=="ponytail")
                check(expanded.snapshot().savingsCents==58000L && expanded.snapshot().entries.size==12)
            }
            pass("expanded wardrobe and independent hair colors survive reopening without adding money")
            val featuresName="features-${System.nanoTime()}.db"
            CompanionStore(targetContext,featuresName).use {features ->
                check(runCatching {features.addCustomGift("",100)}.isFailure)
                check(runCatching {features.addCustomGift("无效",0)}.isFailure)
                features.addCustomGift("旅行纪念",1234)
                check(features.snapshot().entries.isEmpty() && features.snapshot().customGifts.single().priceCents==1234L)
                val gift=features.snapshot().customGifts.single()
                check(runCatching {features.saveForGift(gift.id,1)}.isFailure)
                features.saveForGift(gift.id,1234);features.saveForGift(gift.id,1234)
                check(features.snapshot().giftCount==2 && features.snapshot().savingsCents==2468L)
                check(features.snapshot().entries.all {it.note=="赠送：旅行纪念"})
                pass("custom gifts save without deposits and repeat gifting records the exact chosen amount")
                features.saveForItem("bob",3000);features.saveForItem("bob_palette",500);features.selectHairColor("bob","pink")
                features.saveForItem("hoodie",5000);features.saveForItem("forest",5000)
                features.saveLook("粉色日常")
                val balance=features.snapshot().savingsCents
                features.selectHairColor("bob","black");features.toggleItem("hoodie")
                features.applyLook(features.snapshot().looks.single().id)
                check(features.snapshot().equippedItems["HAIR_COLOR_bob"]=="bob_pink" && features.snapshot().equippedItems["CLOTHES"]=="hoodie")
                check(features.snapshot().equippedItems["BACKGROUND"]=="forest" && features.snapshot().savingsCents==balance)
                pass("saved looks restore appearance and hair colors without deposits or changing the room")
                val backup=features.exportBackup()
                check(features.backupSummary(backup).contains("自定义礼物 1 种"))
                val restoredName="restore-${System.nanoTime()}.db"
                CompanionStore(targetContext,restoredName).use {restored ->
                    restored.addEntry(EntryKind.SAVING,99,"存入储蓄","")
                    restored.restoreBackup(backup)
                    check(restored.snapshot()==features.snapshot())
                    val before=restored.snapshot()
                    val bad=org.json.JSONObject(backup)
                    bad.getJSONObject("tables").getJSONArray("entries").getJSONObject(0).put("cents",-1)
                    check(runCatching {restored.restoreBackup(bad.toString())}.isFailure)
                    check(restored.snapshot()==before)
                    val badColor=org.json.JSONObject(backup)
                    badColor.getJSONObject("tables").getJSONArray("equipment").getJSONObject(0).put("item_id","not-owned")
                    check(runCatching {restored.restoreBackup(badColor.toString())}.isFailure)
                    check(restored.snapshot()==before)
                    check(runCatching {restored.restoreBackup("{}")}.isFailure)
                    check(restored.snapshot()==before)
                    restored.saveForGift(gift.id,1234)
                    check(restored.snapshot().savingsCents==before.savingsCents+1234)
                }
                pass("full backup restores ledger, gifts, wardrobe and looks; invalid restores roll back completely")
            }
            CompanionStore(targetContext,featuresName).use {reopened ->
                check(reopened.snapshot().customGifts.single().name=="旅行纪念" && reopened.snapshot().looks.single().name=="粉色日常")
            }
            pass("custom gifts and saved looks persist after restarting")
            check(GiftCatalog.gifts.size==6 && GiftCatalog.gifts.map {it.id}.distinct().size==6)
            check(SavingsWords.greetings.all {it.contains("存") || it.contains("消费") || it.contains("积累")})
            val art=CharacterArt.get(targetContext)
            check(art.loadedLayerIds.containsAll(listOf("base","blink","bob","pinkhair","silverhair","hoodie","sailor","dress","bow","headphones","crown","bun","ponytail","braid","wave","cardigan","overalls","mintdress","yukata")))
            val colors=listOf("black","brown","pink","silver")
            val hairs=ItemCatalog.items.filter {it.category==ItemCategory.HAIR}
            val colorSheet=android.graphics.Bitmap.createBitmap(1024,1792,android.graphics.Bitmap.Config.ARGB_8888)
            val colorCanvas=android.graphics.Canvas(colorSheet)
            hairs.forEachIndexed {row,hair ->
                val previews=colors.mapIndexed {column,color ->
                    val selected=if(color=="black") emptyMap() else mapOf("HAIR_COLOR_${hair.id}" to "${hair.id}_$color")
                    val image=renderPreview(hair,art,selected)
                    val pixels=IntArray(256*256)
                    image.readPixels(pixels)
                    colorCanvas.drawBitmap(android.graphics.Bitmap.createBitmap(pixels,256,256,android.graphics.Bitmap.Config.ARGB_8888),column*256f,row*256f,null)
                    pixels
                }
                for(i in previews.indices) for(j in i+1 until previews.size) check(!previews[i].contentEquals(previews[j])) {"${hair.id} colors do not change rendered pixels"}
                val reset=IntArray(256*256);renderPreview(hair,art).readPixels(reset)
                check(reset.contentEquals(previews[0]))
                val palette=ItemCatalog.items.first {it.requiredHairId==hair.id}
                check(previewEquipment(palette,mapOf("HAIR_COLOR_${hair.id}" to "${hair.id}_pink"))[palette.slot]=="${hair.id}_pink")
            }
            java.io.FileOutputStream(java.io.File(targetContext.getExternalFilesDir(null),"hair-colors-validation.png")).use {colorSheet.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            pass("every hairstyle visibly changes in all four colors and cached previews restore black")
            val poses=(0..7).map {frame ->
                android.graphics.Bitmap.createBitmap(864,672,android.graphics.Bitmap.Config.ARGB_8888).also {image ->
                    val c=android.graphics.Canvas(image);c.scale(6f,6f);art.draw(c,mapOf("HAIR" to "bob","CLOTHES" to "sailor"),frame,true)
                }
            }
            val bounds=poses.map {CharacterArt.opaqueBounds(it)}
            check(bounds.map {it.width()}.toSet().size==1 && bounds.maxOf {it.height()}-bounds.minOf {it.height()}<=1)
            check(bounds.map {it.top}.toSet().size>1)
            val thanksSheet=android.graphics.Bitmap.createBitmap(1728,672,android.graphics.Bitmap.Config.ARGB_8888)
            android.graphics.Canvas(thanksSheet).apply {drawColor(android.graphics.Color.rgb(255,249,239));drawBitmap(poses[0],0f,0f,null);drawBitmap(poses[3],864f,0f,null)}
            java.io.FileOutputStream(java.io.File(targetContext.getExternalFilesDir(null),"thanks-validation.png")).use {thanksSheet.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            pass("thank-you animation moves vertically without flattening the character")
            val icon=android.graphics.Bitmap.createBitmap(720,720,android.graphics.Bitmap.Config.ARGB_8888)
            targetContext.applicationInfo.loadIcon(targetContext.packageManager).apply {setBounds(0,0,720,720);draw(android.graphics.Canvas(icon))}
            java.io.FileOutputStream(java.io.File(targetContext.getExternalFilesDir(null),"app-icon-current.png")).use {icon.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            val sheet=android.graphics.Bitmap.createBitmap(1728,1008,android.graphics.Bitmap.Config.ARGB_8888)
            val canvas=android.graphics.Canvas(sheet)
            canvas.drawColor(android.graphics.Color.rgb(255,249,239))
            val outfits=listOf(
                mapOf("HAIR" to "bun","CLOTHES" to "cardigan","SKIN" to "skin_ivory"),
                mapOf("HAIR" to "ponytail","CLOTHES" to "overalls","SKIN" to "skin_ivory"),
                mapOf("HAIR" to "braid","CLOTHES" to "mintdress","SKIN" to "skin_ivory"),
                mapOf("HAIR" to "wave","CLOTHES" to "yukata","SKIN" to "skin_ivory"),
                mapOf("HAIR" to "bun","CLOTHES" to "yukata","SKIN" to "skin_peach","HAIR_COLOR_bun" to "bun_pink"),
                mapOf("HAIR" to "ponytail","CLOTHES" to "cardigan","SKIN" to "skin_wheat","HAIR_COLOR_ponytail" to "ponytail_brown"),
                mapOf("HAIR" to "braid","CLOTHES" to "overalls","HAIR_COLOR_braid" to "braid_silver"),
                mapOf("HAIR" to "wave","CLOTHES" to "mintdress","HAIR_COLOR_wave" to "wave_pink"),
                emptyMap(),mapOf("HAIR" to "bob","CLOTHES" to "sailor","SKIN" to "skin_ivory","HAIR_COLOR_bob" to "bob_brown"),
                mapOf("HAIR" to "pinkhair","CLOTHES" to "dress"),mapOf("HAIR" to "silverhair","CLOTHES" to "hoodie","ACCESSORY" to "bow"))
            outfits.forEachIndexed {index,equipment ->
                canvas.save();canvas.translate((index%4)*432f,(index/4)*336f);canvas.scale(3f,3f)
                art.draw(canvas,equipment,if(index==11) 3 else 0,index==11);canvas.restore()
            }
            java.io.FileOutputStream(java.io.File(targetContext.getExternalFilesDir(null),"character-validation.png")).use {sheet.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            check(CharacterArt.opaqueBounds(sheet).width()>0)
            pass("all character PNG layers load and complete outfits render")
            finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "\nOK (${completed.size} database checks)\n"); putInt("checks", completed.size) })
        } catch (error: Throwable) {
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("stream", "\nFAILED: ${error.stackTraceToString()}\n"); putString("failure", error.toString()) })
        }
    }
}
