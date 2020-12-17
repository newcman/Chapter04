package com.hprof.bitmap

import com.hprof.bitmap.entry.BitmapInstance
import com.hprof.bitmap.entry.DuplicatedCollectInfo
import com.hprof.bitmap.utils.HahaHelper
import com.squareup.haha.perflib.ArrayInstance
import com.squareup.haha.perflib.HprofParser
import com.squareup.haha.perflib.Instance
import com.squareup.haha.perflib.Snapshot
import com.squareup.haha.perflib.io.HprofBuffer
import com.squareup.haha.perflib.io.MemoryMappedFileBuffer
import java.io.File
import java.io.FileNotFoundException
import java.util.*

/**
 * 重复图片检测
 */
object DuplicatedBitmapHelper {
    /**
     * 获取重复图片
     */
    fun getDuplicatedBitmap(heapDumpFile: String): List<DuplicatedCollectInfo>? {
        val heapDumpFile = File(heapDumpFile)

        val isExists = heapDumpFile.exists()
        if (!isExists) {
            throw FileNotFoundException("heapDumpFile=$heapDumpFile is not  exists")
        }

        val buffer: HprofBuffer = MemoryMappedFileBuffer(heapDumpFile)
        val parser = HprofParser(buffer)
        val snapshot = parser.parse()
        snapshot.computeDominators()

        val bitmapClass = snapshot.findClass("android.graphics.Bitmap")

        // 只分析 default 和 app
        // 只分析 default 和 app
        val defaultHeap = snapshot.getHeap("default")
        val appHeap = snapshot.getHeap("app")
        // 从 heap 中获取 bitmap instance 实例
        // 从 heap 中获取 bitmap instance 实例
        val defaultBmInstance = bitmapClass.getHeapInstances(defaultHeap.id)
        val appBmInstance = bitmapClass.getHeapInstances(appHeap.id)
        defaultBmInstance.addAll(appBmInstance)

        return collectSameBitmap(snapshot, defaultBmInstance)
    }


    private fun collectSameBitmap(snapshot: Snapshot, bmInstanceList: List<Instance>): List<DuplicatedCollectInfo>? {
        val collectSameMap: MutableMap<String, MutableList<Instance>> = HashMap()
        val duplicatedCollectInfos = ArrayList<DuplicatedCollectInfo>()
        // 收集
        for (instance in bmInstanceList) {
            val classFieldList = HahaHelper.classInstanceValues(instance)
            val arrayInstance = HahaHelper.fieldValue<ArrayInstance>(classFieldList, "mBuffer")
            val mBufferByte = HahaHelper.getByteArray(arrayInstance)
            val mBufferHashCode = Arrays.hashCode(mBufferByte)
            val hashKey = mBufferHashCode.toString()
            if (collectSameMap.containsKey(hashKey)) {
                collectSameMap[hashKey]!!.add(instance)
            } else {
                val bmList: MutableList<Instance> = ArrayList()
                bmList.add(instance)
                collectSameMap.put(hashKey, bmList)
            }
        }
        // 去除只有一例的
        val it: MutableIterator<Map.Entry<String, List<Instance>>> = collectSameMap.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (entry.value.size <= 1) {
                it.remove()
            }
        }
        // 留下重复的图片，创建 duplicatedCollectInfo 对象存入数组中
        collectSameMap.forEach { key, value ->
            val info = DuplicatedCollectInfo(key)
            for (instance in value) {
                info.addBitmapInstance(BitmapInstance(snapshot, key, instance))
            }
            info.internalSetValue()
            duplicatedCollectInfos.add(info)
        }
        return duplicatedCollectInfos
    }
}