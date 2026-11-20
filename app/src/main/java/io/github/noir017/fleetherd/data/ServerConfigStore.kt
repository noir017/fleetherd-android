package io.github.noir017.fleetherd.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ServerProfile(
    val id: String,
    val name: String,
    val url: String,
    val ssoHosts: List<String> = emptyList()
)

class ServerConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun list(): List<ServerProfile> {
        val raw = prefs.getString(KEY_SERVERS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id")
                    val url = obj.optString("url")
                    if (id.isBlank() || url.isBlank()) continue
                    val name = obj.optString("name").ifBlank { url }
                    val ssoArr = obj.optJSONArray("ssoHosts")
                    val sso = buildList {
                        if (ssoArr != null) {
                            for (j in 0 until ssoArr.length()) {
                                val h = ssoArr.optString(j).trim()
                                if (h.isNotEmpty()) add(h)
                            }
                        }
                    }
                    add(ServerProfile(id = id, name = name, url = url, ssoHosts = sso))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun active(): ServerProfile? {
        val all = list()
        if (all.isEmpty()) return null
        val activeId = prefs.getString(KEY_ACTIVE_ID, null)
        return all.firstOrNull { it.id == activeId } ?: all.first()
    }

    fun setActive(id: String) {
        prefs.edit().putString(KEY_ACTIVE_ID, id).apply()
    }

    fun upsert(name: String, url: String, ssoHosts: List<String>, existingId: String? = null): ServerProfile {
        val current = list().toMutableList()
        val id = existingId ?: current.firstOrNull { it.url == url }?.id ?: UUID.randomUUID().toString()
        val cleanName = name.trim().ifBlank { url.removePrefix("https://").removePrefix("http://") }
        val profile = ServerProfile(id = id, name = cleanName, url = url, ssoHosts = ssoHosts)
        val idx = current.indexOfFirst { it.id == id }
        if (idx >= 0) current[idx] = profile else current.add(profile)
        saveAll(current, id)
        return profile
    }

    fun delete(id: String) {
        val remaining = list().filterNot { it.id == id }
        val nextActive = if (prefs.getString(KEY_ACTIVE_ID, null) == id) {
            remaining.firstOrNull()?.id
        } else {
            prefs.getString(KEY_ACTIVE_ID, null)
        }
        saveAll(remaining, nextActive)
    }

    private fun saveAll(items: List<ServerProfile>, activeId: String?) {
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject()
                .put("id", item.id)
                .put("name", item.name)
                .put("url", item.url)
                .put("ssoHosts", JSONArray(item.ssoHosts))
            arr.put(obj)
        }
        prefs.edit()
            .putString(KEY_SERVERS, arr.toString())
            .putString(KEY_ACTIVE_ID, activeId)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "fleetherd_servers"
        private const val KEY_SERVERS = "servers_json"
        private const val KEY_ACTIVE_ID = "active_server_id"
    }
}
