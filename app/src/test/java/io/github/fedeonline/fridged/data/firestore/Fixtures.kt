package io.github.fedeonline.fridged.data.firestore

import com.google.firebase.Timestamp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

/**
 * Loads a fixture from docs/fixtures (Firestore REST JSON) and decodes its fields into
 * the map `DocumentSnapshot.getData()` would return.
 */
object Fixtures {
    fun fields(name: String): Map<String, Any?> {
        val text = checkNotNull(javaClass.classLoader?.getResource(name)) { "Missing fixture $name" }.readText()
        return Json.parseToJsonElement(text).jsonObject.getValue("fields").jsonObject.mapValues { decode(it.value) }
    }

    private fun decode(value: JsonElement): Any? {
        val (type, content) = (value as JsonObject).entries.single()
        return when (type) {
            "nullValue" -> null
            "booleanValue" -> content.jsonPrimitive.boolean
            "integerValue" -> content.jsonPrimitive.content.toLong()
            "doubleValue" -> content.jsonPrimitive.double
            "stringValue" -> content.jsonPrimitive.content
            "timestampValue" -> Timestamp(Instant.parse(content.jsonPrimitive.content))
            else -> error("Unsupported Firestore type $type in fixture").also { check(content != JsonNull) }
        }
    }
}
