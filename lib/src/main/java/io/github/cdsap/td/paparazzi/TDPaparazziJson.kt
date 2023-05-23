package io.github.cdsap.td.paparazzi

import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.TestName
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import java.util.Date

internal object TDPaparazziJson {
    val moshi = Moshi.Builder()
        .add(Date::class.java, Rfc3339DateJsonAdapter())
        .add(this)
        .build()!!

    val listOfShotsAdapter: JsonAdapter<List<Snapshot>> =
        moshi
            .adapter<List<Snapshot>>(
                Types.newParameterizedType(List::class.java, Snapshot::class.java)
            )
            .indent("  ")

    val listOfStringsAdapter: JsonAdapter<List<String>> =
        moshi
            .adapter<List<String>>(
                Types.newParameterizedType(List::class.java, String::class.java)
            )
            .indent("  ")

    @ToJson
    fun testNameToJson(testName: TestName): String {
        return "${testName.packageName}.${testName.className}#${testName.methodName}"
    }

    @FromJson
    fun testNameFromJson(json: String): TestName {
        val regex = Regex("(.*)\\.([^.]*)#([^.]*)")
        val (packageName, className, methodName) = regex.matchEntire(json)!!.destructured
        return TestName(packageName, className, methodName)
    }
}
