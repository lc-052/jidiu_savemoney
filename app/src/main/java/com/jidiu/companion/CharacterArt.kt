package com.jidiu.companion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.LruCache

/** One consistent PNG character anatomy; outfits replace the body, wigs share its face. */
internal class CharacterArt(context: Context) {
    private val frameWidth=1111f
    private val frameHeight=1417f
    private val hairIds=ItemCatalog.items.filter {it.category==ItemCategory.HAIR}.map {it.id}.toSet()
    private val outfitIds=ItemCatalog.items.filter {it.category==ItemCategory.CLOTHES}.map {it.id}.toSet()
    private val alignedHairIds=setOf("bun","ponytail","braid","wave")
    private val paint=Paint().apply {isAntiAlias=false;isFilterBitmap=false}
    private val layers=buildMap<String,Bitmap> {
        val names=listOf("base","blink")+hairIds+outfitIds
        for(id in names) context.assets.open("characters/reference_$id.png").use {
            put(id,BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply {inScaled=false;inSampleSize=2})!!)
        }
        for(id in listOf("bow","headphones","crown")) context.assets.open("characters/$id.png").use {
            put(id,BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply {inSampleSize=2})!!)
        }
    }
    private val base=layers.getValue("base")
    private val bodyBounds=opaqueBounds(base)
    private val layerBounds=layers.mapValues {opaqueBounds(it.value)}
    private val cache=object: LruCache<String,Bitmap>(48*1024) {
        override fun sizeOf(key: String,value: Bitmap)=maxOf(1,value.allocationByteCount/1024)
    }
    private val hairRows=layers.filterKeys {it in hairIds}.mapValues {(_,image)->
        val pixels=IntArray(image.width*image.height)
        image.getPixels(pixels,0,image.width,0,0,image.width,image.height)
        Array(image.height) {y->
            var left=image.width;var right=-1
            for(x in 0 until image.width) if(Color.alpha(pixels[y*image.width+x])>=96) {left=minOf(left,x);right=maxOf(right,x)}
            intArrayOf(left,right+1)
        }
    }
    internal val loadedLayerIds get()=layers.keys

    fun draw(canvas: Canvas,equipment: Map<String,String>,frame: Int,thanking: Boolean) {
        canvas.save()
        val bounce=if(thanking) listOf(0f,-1f,-2.5f,-3.5f,-2.5f,-1f,0f,0f)[frame%8] else if(frame%8<4) -.4f else 0f
        canvas.translate(72f,98f+bounce)
        val scale=68f/(bodyBounds.height()*frameHeight/base.height)
        canvas.scale(scale,scale)
        canvas.translate(-bodyBounds.exactCenterX()*frameWidth/base.width,-bodyBounds.bottom*frameHeight/base.height)
        val outfit=equipment[ItemCategory.CLOTHES.name]?.takeIf {it in outfitIds} ?: "base"
        val pose=if(frame%24==17 || thanking && frame%8 in 2..4) "blink" else "base"
        val hair=equipment[ItemCategory.HAIR.name]
        val head=variant("head/$pose/$hair") {
            val colored=layers.getValue(pose)
            if(hair==null) colored else fitHead(colored,hair)
        }
        val body=layers.getValue(outfit)
        // All bodies normalize to the same canvas. Outfit faces never replace the shared face.
        val split=535f
        canvas.drawBitmap(body,Rect(0,(body.height*split/frameHeight).toInt(),body.width,body.height),
            RectF(0f,split,frameWidth,frameHeight),paint)
        canvas.drawBitmap(head,Rect(0,0,head.width,(head.height*split/frameHeight).toInt()),
            RectF(0f,0f,frameWidth,split),paint)
        hair?.let {id->
            val color=equipment["HAIR_COLOR_$id"]?.substringAfterLast('_') ?: "black"
            drawLayer(canvas,id,hairTarget(id),color,aligned=id in alignedHairIds)
        }
        equipment[ItemCategory.ACCESSORY.name]?.let {id->
            val target=when(id) {
                "bow"->RectF(710f,170f,842f,285f)
                "headphones"->RectF(285f,130f,825f,490f)
                else->RectF(430f,35f,675f,120f)
            }
            drawLayer(canvas,id,target)
        }
        canvas.restore()
    }
    private fun hairTarget(id: String)=when(id) {
        "pinkhair"->RectF(135f,-45f,975f,570f)
        "silverhair"->RectF(285f,-45f,825f,600f)
        else->RectF(285f,-45f,825f,570f)
    }
    private fun drawLayer(canvas: Canvas,id: String,target: RectF,color: String="black",aligned: Boolean=false) {
        val original=layers[id] ?: return
        val image=if(color=="black") original else variant("hair/$id/$color") {recolorHair(original,color)}
        if(aligned) canvas.drawBitmap(image,null,alignedHairTarget(),paint)
        else canvas.drawBitmap(image,layerBounds.getValue(id),target,paint)
    }
    private fun alignedHairTarget(): RectF {
        val reference=layers.getValue("bob")
        val bounds=layerBounds.getValue("bob")
        val target=hairTarget("bob")
        val left=bounds.left*frameWidth/reference.width
        val top=bounds.top*frameHeight/reference.height
        val width=bounds.width()*frameWidth/reference.width
        val height=bounds.height()*frameHeight/reference.height
        val sx=target.width()/width;val sy=target.height()/height
        val originX=target.left-left*sx;val originY=target.top-top*sy
        return RectF(originX,originY,originX+frameWidth*sx,originY+frameHeight*sy)
    }
    private fun fitHead(source: Bitmap,hair: String): Bitmap {
        val rows=hairRows[hair] ?: return source
        val aligned=hair in alignedHairIds
        val bounds=if(aligned) Rect(0,0,layers.getValue(hair).width,layers.getValue(hair).height) else layerBounds.getValue(hair)
        val target=if(aligned) alignedHairTarget() else hairTarget(hair)
        return transform(source) {x,y,pixel->
            val nx=x*frameWidth/source.width
            val ny=y*frameHeight/source.height
            if(ny>=535 || ny<target.top) pixel else {
                val row=(bounds.top+(ny-target.top)*bounds.height()/target.height()).toInt().coerceIn(0,rows.lastIndex)
                val edge=rows[row]
                val left=target.left+(edge[0]-bounds.left)*target.width()/bounds.width()
                val right=target.left+(edge[1]-bounds.left)*target.width()/bounds.width()
                if(edge[1]<=edge[0] || nx<left || nx>=right) Color.TRANSPARENT else pixel
            }
        }
    }
    private fun recolorHair(image: Bitmap,color: String): Bitmap {
        val target=when(color) {"brown"->Color.rgb(128,100,78);"pink"->Color.rgb(217,132,166);"silver"->Color.rgb(174,183,200);else->Color.rgb(45,47,57)}
        return transform(image) {_,_,pixel->
            val light=(Color.red(pixel)+Color.green(pixel)+Color.blue(pixel))/3
            if(light<22) pixel else {
                val value=(.35f+light/140f).coerceAtMost(1.35f)
                Color.argb(Color.alpha(pixel),(Color.red(target)*value).toInt().coerceIn(0,255),(Color.green(target)*value).toInt().coerceIn(0,255),(Color.blue(target)*value).toInt().coerceIn(0,255))
            }
        }
    }
    private fun variant(key: String,create: ()->Bitmap): Bitmap=cache.get(key) ?: create().also {cache.put(key,it)}
    private fun transform(source: Bitmap,change: (Int,Int,Int)->Int): Bitmap {
        val pixels=IntArray(source.width*source.height)
        source.getPixels(pixels,0,source.width,0,0,source.width,source.height)
        for(i in pixels.indices) if(Color.alpha(pixels[i])>0) pixels[i]=change(i%source.width,i/source.width,pixels[i])
        return Bitmap.createBitmap(pixels,source.width,source.height,Bitmap.Config.ARGB_8888)
    }
    companion object {
        private var shared: CharacterArt?=null
        @Synchronized fun get(context: Context)=shared ?: CharacterArt(context.applicationContext).also {shared=it}
        internal fun opaqueBounds(image: Bitmap): Rect {
            val pixels=IntArray(image.width*image.height)
            image.getPixels(pixels,0,image.width,0,0,image.width,image.height)
            var left=image.width;var top=image.height;var right=0;var bottom=0
            for(i in pixels.indices) if(Color.alpha(pixels[i])>=96) {
                val x=i%image.width;val y=i/image.width
                left=minOf(left,x);top=minOf(top,y);right=maxOf(right,x+1);bottom=maxOf(bottom,y+1)
            }
            require(right>left && bottom>top) {"角色素材没有可见内容。"}
            return Rect(left,top,right,bottom)
        }
    }
}
