package com.localflow.player

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.media.*
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/** Offline test fixtures. These are compiled only into the test APK. */
object TestMedia {
    private data class Packet(val bytes: ByteArray,val time: Long,val flags: Int)
    private data class Track(val format: MediaFormat,val packets: List<Packet>)
    fun create(context: Context) {
        val audio=encode(false)
        val video=encode(true)
        val audioFile=File(context.cacheDir,"midnight-qa.m4a")
        mux(audioFile,listOf(audio))
        addArt(audioFile)
        val videoFile=File(context.cacheDir,"midnight-qa.mp4")
        mux(videoFile,listOf(video,audio))
        publish(context,audioFile,false)
        publish(context,videoFile,true)
    }
    private fun encode(video: Boolean): Track {
        val mime=if(video) "video/avc" else "audio/mp4a-latm"
        val format=if(video) MediaFormat.createVideoFormat(mime,320,180).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE,180_000); setInteger(MediaFormat.KEY_FRAME_RATE,2); setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1)
        } else MediaFormat.createAudioFormat(mime,22050,1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE,48_000); setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,2048)
        }
        val codec=MediaCodec.createEncoderByType(mime)
        val packets=ArrayList<Packet>()
        var outFormat: MediaFormat?=null
        try {
            codec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE); codec.start()
            var input=0L; var ended=false; var outputEnded=false
            val duration=90_000_000L
            val deadline=android.os.SystemClock.elapsedRealtime()+120_000
            val info=MediaCodec.BufferInfo()
            while(!outputEnded) {
                check(android.os.SystemClock.elapsedRealtime()<deadline) { "Encoder timeout" }
                if(!ended) {
                    val i=codec.dequeueInputBuffer(10_000)
                    if(i>=0) {
                        val time=if(video) input*500_000 else input*1_000_000/22050
                        val b=codec.getInputBuffer(i)!!; b.clear()
                        if(time>=duration) { codec.queueInputBuffer(i,0,0,time,MediaCodec.BUFFER_FLAG_END_OF_STREAM); ended=true }
                        else if(video) {
                            val frame=ByteArray(320*180*3/2)
                            for(y in 0 until 180) for(x in 0 until 320) frame[y*320+x]=(40+(x+y+input.toInt())%130).toByte()
                            java.util.Arrays.fill(frame,320*180,320*180+320*180/4,175.toByte())
                            java.util.Arrays.fill(frame,320*180+320*180/4,frame.size,120.toByte())
                            b.put(frame); codec.queueInputBuffer(i,0,frame.size,time,0); input++
                        } else {
                            b.order(ByteOrder.LITTLE_ENDIAN)
                            repeat(1024) { j -> b.putShort((sin(2*PI*220*(input+j)/22050)*700).toInt().toShort()) }
                            codec.queueInputBuffer(i,0,2048,time,0); input+=1024
                        }
                    }
                }
                var o=codec.dequeueOutputBuffer(info,10_000)
                if(o==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) outFormat=codec.outputFormat
                while(o>=0) {
                    if(info.size>0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG==0) {
                        val b=codec.getOutputBuffer(o)!!; b.position(info.offset); b.limit(info.offset+info.size)
                        val data=ByteArray(info.size); b.get(data)
                        packets+=Packet(data,info.presentationTimeUs,info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM.inv())
                    }
                    outputEnded=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0
                    codec.releaseOutputBuffer(o,false)
                    if(outputEnded) break
                    o=codec.dequeueOutputBuffer(info,0)
                }
            }
        } finally { runCatching { codec.stop() }; codec.release() }
        return Track(checkNotNull(outFormat),packets)
    }
    private fun mux(file: File,tracks: List<Track>) {
        val muxer=MediaMuxer(file.absolutePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        try {
            val ids=tracks.map { muxer.addTrack(it.format) }; muxer.start()
            tracks.forEachIndexed { i,t -> t.packets.forEach { p ->
                val info=MediaCodec.BufferInfo().apply { set(0,p.bytes.size,p.time,p.flags) }
                muxer.writeSampleData(ids[i],ByteBuffer.wrap(p.bytes),info)
            } }
            muxer.stop()
        } finally { muxer.release() }
    }
    private fun atom(type: String,data: ByteArray): ByteArray = ByteBuffer.allocate(data.size+8).putInt(data.size+8).put(type.toByteArray(Charsets.ISO_8859_1)).put(data).array()
    private fun metadata(type: String,data: ByteArray,format: Int): ByteArray = atom(type,atom("data",ByteBuffer.allocate(data.size+8).putInt(format).putInt(0).put(data).array()))
    private fun addArt(file: File) {
        val cover=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888)
        val c=Canvas(cover); c.drawColor(Color.rgb(33,28,51))
        val p=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.rgb(89,72,125) }; c.drawCircle(365f,145f,230f,p)
        p.color=Color.rgb(188,171,255); p.style=Paint.Style.STROKE; p.strokeWidth=3f
        repeat(5) { c.drawCircle(255f,240f,45f+it*29,p) }
        p.style=Paint.Style.FILL; p.color=Color.WHITE; p.textSize=36f; p.typeface=Typeface.create(Typeface.SANS_SERIF,Typeface.BOLD)
        c.drawText("MIDNIGHT",38f,429f,p); p.textSize=18f; c.drawText("LOCALFLOW / QA",40f,465f,p)
        val stream=ByteArrayOutputStream(); cover.compress(Bitmap.CompressFormat.JPEG,85,stream); cover.recycle()
        val ilst=atom("ilst",metadata("covr",stream.toByteArray(),13)+metadata("©nam","Horizonte violeta".toByteArray(),1)+metadata("©ART","LocalFlow QA".toByteArray(),1)+metadata("©alb","Midnight Sessions".toByteArray(),1))
        val udta=atom("udta",atom("meta",ByteArray(4)+ilst))
        val bytes=file.readBytes(); var offset=0; var mdat=-1
        while(offset+8<=bytes.size) {
            val shortSize=ByteBuffer.wrap(bytes,offset,4).int
            val header=if(shortSize==1) 16 else 8
            val size=if(shortSize==1) ByteBuffer.wrap(bytes,offset+8,8).long.toInt() else if(shortSize==0) bytes.size-offset else shortSize
            val type=String(bytes,offset+4,4,Charsets.ISO_8859_1)
            check(size>=header && offset+size<=bytes.size) { "Invalid MP4 atom $type" }
            if(type=="mdat") mdat=offset
            if(type=="moov") {
                check(mdat in 0 until offset)
                file.writeBytes(bytes.copyOfRange(0,offset)+atom("moov",bytes.copyOfRange(offset+header,offset+size)+udta)+bytes.copyOfRange(offset+size,bytes.size))
                return
            }
            offset+=size
        }
        error("MP4 moov missing")
    }
    private fun publish(context: Context,file: File,video: Boolean) {
        val collection=if(video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val values=ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME,if(video) "LocalFlow QA Video.mp4" else "LocalFlow QA Horizonte.m4a")
            put(MediaStore.MediaColumns.MIME_TYPE,if(video) "video/mp4" else "audio/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH,if(video) "Movies/LocalFlow-QA/" else "Music/LocalFlow-QA/")
            put(MediaStore.MediaColumns.IS_PENDING,1)
            put(MediaStore.MediaColumns.TITLE,if(video) "Paisagem Midnight" else "Horizonte violeta")
        }
        val uri=checkNotNull(context.contentResolver.insert(collection,values))
        context.contentResolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
        context.contentResolver.update(uri,ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING,0) },null,null)
        file.delete()
    }
}
