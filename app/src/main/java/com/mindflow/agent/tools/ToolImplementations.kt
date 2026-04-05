package com.mindflow.agent.tools

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Built-in tool implementations for the agent system
 */
class WebSearchTool(private val context: Context) : Tool {
    override val name = "web_search"
    override val description = "Search the web for information"
    override val inputSchema = """{"type":"object","properties":{"query":{"type":"string","description":"Search query"}},"required":["query"]}"""
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    suspend fun execute(query: String): String = withContext(Dispatchers.IO) {
        try {
            // Using DuckDuckGo instant answer API (free, no API key required)
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_redirect=1")
                .get()
                .build()
            
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext errorResult("Empty response")
                parseDuckDuckGoResponse(body)
            } else {
                errorResult("Search failed: ${response.code}")
            }
        } catch (e: Exception) {
            errorResult("Search error: ${e.message}")
        }
    }
    
    private fun parseDuckDuckGoResponse(json: String): String {
        return try {
            val jsonObj = Json.parseToJsonElement(json).jsonObject
            val heading = jsonObj["Heading"]?.jsonPrimitive?.content ?: ""
            val abstract = jsonObj["Abstract"]?.jsonPrimitive?.content ?: ""
            val relatedTopics = jsonObj["RelatedTopics"]?.jsonArray?.take(3)
                ?.mapNotNull { it.jsonObject["Text"]?.jsonPrimitive?.content }
                ?.joinToString("\n") ?: ""
            
            buildString {
                if (heading.isNotEmpty()) append("Topic: $heading\n")
                if (abstract.isNotEmpty()) append("Summary: $abstract\n")
                if (relatedTopics.isNotEmpty()) append("Related:\n$relatedTopics")
            }.ifEmpty { "No results found" }
        } catch (e: Exception) {
            "Error parsing response: ${e.message}"
        }
    }
    
    private fun errorResult(message: String) = """{"error": "$message"}"""
}

class CalculatorTool : Tool {
    override val name = "calculator"
    override val description = "Evaluate mathematical expressions"
    override val inputSchema = """{"type":"object","properties":{"expression":{"type":"string","description":"Mathematical expression (e.g., 2+2, sqrt(16), sin(pi/2))"}},"required":["expression"]}"""
    
    fun execute(expression: String): String {
        return try {
            val sanitized = expression.replace("[^0-9+\\-*/.()^% sqrt sin cos tan log ln pi e]".toRegex(), "")
            val result = evaluateMathExpression(sanitized)
            """{"result": $result, "expression": "$sanitized"}"""
        } catch (e: Exception) {
            """{"error": "${e.message}"}"""
        }
    }
    
    private fun evaluateMathExpression(expr: String): Double {
        // Replace math constants
        var expression = expr
            .replace("pi", "(${Math.PI})")
            .replace("e", "(${Math.E})")
        
        // Replace functions
        expression = expression
            .replace(Regex("sqrt\\(([^)]+)\\)")) { 
                "(${Math.sqrt(it.groupValues[1].toDouble())})" 
            }
            .replace(Regex("sin\\(([^)]+)\\)")) {
                "(${Math.sin(Math.toRadians(it.groupValues[1].toDouble()))})"
            }
            .replace(Regex("cos\\(([^)]+)\\)")) {
                "(${Math.cos(Math.toRadians(it.groupValues[1].toDouble()))})"
            }
            .replace(Regex("tan\\(([^)]+)\\)")) {
                "(${Math.tan(Math.toRadians(it.groupValues[1].toDouble()))})"
            }
            .replace(Regex("log\\(([^)]+)\\)")) {
                "(${Math.log10(it.groupValues[1].toDouble())})"
            }
            .replace(Regex("ln\\(([^)]+)\\)")) {
                "(${Math.log(it.groupValues[1].toDouble())})"
            }
            .replace("^", "**")
        
        return evaluate(expression)
    }
    
    private fun evaluate(expr: String): Double {
        return object {
            var pos = -1
            var ch = 0
            
            fun nextChar() {
                ch = if (++pos < expr.length) expr[pos].code else -1
            }
            
            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }
            
            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < expr.length) throw RuntimeException("Unexpected: ${ch.toChar()}")
                return x
            }
            
            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> x += parseTerm()
                        eat('-'.code) -> x -= parseTerm()
                        else -> return x
                    }
                }
            }
            
            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> x /= parseFactor()
                        eat('%'.code) -> x %= parseFactor()
                        else -> return x
                    }
                }
            }
            
            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                
                var x = 0.0
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = expr.substring(startPos, pos).toDouble()
                }
                return x
            }
        }.parse()
    }
}

class TextSummarizerTool : Tool {
    override val name = "text_summarizer"
    override val description = "Summarize long text into a concise summary"
    override val inputSchema = """{"type":"object","properties":{"text":{"type":"string","description":"Text to summarize"},"maxLength":{"type":"integer","description":"Maximum summary length","default":200}},"required":["text"]}"""
    
    fun execute(text: String, maxLength: Int = 200): String {
        return try {
            // Simple extractive summarization
            val sentences = text.split(Regex("[.!?]+")).filter { it.trim().isNotEmpty() }
            if (sentences.size <= 2) {
                return """{"summary": "${text.take(maxLength)}", "originalLength": ${text.length}}"""
            }
            
            // Score sentences by length and position
            val scored = sentences.mapIndexed { index, sentence ->
                val words = sentence.trim().split("\\s+".toRegex())
                val score = words.size * 0.5 + (sentences.size - index) * 0.3
                sentence.trim() to score
            }.sortedByDescending { it.second }
            
            val summary = scored.take(3).joinToString(". ") { it.first }
            """{"summary": "${summary.take(maxLength)}", "originalLength": ${text.length}, "sentenceCount": ${sentences.size}}"""
        } catch (e: Exception) {
            """{"error": "${e.message}"}"""
        }
    }
}

class KnowledgeQueryTool(
    private val knowledgeRepository: com.mindflow.domain.repository.KnowledgeRepository
) : Tool {
    override val name = "knowledge_query"
    override val description = "Query the local knowledge base for relevant information"
    override val inputSchema = """{"type":"object","properties":{"query":{"type":"string","description":"Search query"},"limit":{"type":"integer","description":"Maximum results","default":5}},"required":["query"]}"""
    
    suspend fun execute(query: String, limit: Int = 5): String {
        return try {
            val results = knowledgeRepository.queryKnowledge(query, limit).getOrNull() ?: emptyList()
            if (results.isEmpty()) {
                return """{"results": [], "query": "$query"}"""
            }
            
            val resultsJson = results.joinToString(",") { doc ->
                """{"id": "${doc.id}", "title": "${doc.title}", "content": "${doc.content.take(200)}", "source": "${doc.source}"}"""
            }
            """{"results": [$resultsJson], "count": ${results.size}, "query": "$query"}"""
        } catch (e: Exception) {
            """{"error": "${e.message}"}"""
        }
    }
}

class DateTimeTool : Tool {
    override val name = "datetime"
    override val description = "Get current date and time information"
    override val inputSchema = """{"type":"object","properties":{"format":{"type":"string","description":"Output format (iso, readable, unix)","default":"readable"}},"required":[]}"""
    
    fun execute(format: String = "readable"): String {
        val now = System.currentTimeMillis()
        val date = java.util.Date(now)
        
        return when (format) {
            "iso" -> {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                """{"iso": "${sdf.format(date)}", "timestamp": $now}"""
            }
            "unix" -> """{"unix": ${now / 1000}, "timestamp": $now}"""
            else -> {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                """{"readable": "${sdf.format(date)}", "timestamp": $now, "timezone": "${java.util.TimeZone.getDefault().id}"}"""
            }
        }
    }
}

class UrlFetchTool(private val context: Context) : Tool {
    override val name = "url_fetch"
    override val description = "Fetch and extract content from a URL"
    override val inputSchema = """{"type":"object","properties":{"url":{"type":"string","description":"URL to fetch"},"maxLength":{"type":"integer","description":"Maximum content length","default":2000}},"required":["url"]}"""
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    suspend fun execute(url: String, maxLength: Int = 2000): String = withContext(Dispatchers.IO) {
        try {
            // Basic URL validation
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return@withContext """{"error": "Invalid URL protocol"}"""
            }
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (compatible; MindFlow/1.0)")
                .get()
                .build()
            
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext """{"error": "Fetch failed: ${response.code}"}"""
            }
            
            val body = response.body?.string() ?: return@withContext """{"error": "Empty response"}"""
            val text = extractText(body).take(maxLength)
            
            """{"url": "$url", "content": "${text.replace("\"", "'")}", "length": ${body.length}}"""
        } catch (e: Exception) {
            """{"error": "${e.message}"}"""
        }
    }
    
    private fun extractText(html: String): String {
        // Simple HTML tag removal
        return html
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

class ConverterTool : Tool {
    override val name = "converter"
    override val description = "Convert between different units or formats"
    override val inputSchema = """{"type":"object","properties":{"type":{"type":"string","description":"Conversion type (length, weight, temp, currency)"},"value":{"type":"number","description":"Value to convert"},"from":{"type":"string","description":"Source unit"},"to":{"type":"string","description":"Target unit"}},"required":["type","value","from","to"]}"""
    
    fun execute(type: String, value: Double, from: String, to: String): String {
        return try {
            val result = when (type.lowercase()) {
                "length" -> convertLength(value, from, to)
                "weight" -> convertWeight(value, from, to)
                "temp", "temperature" -> convertTemperature(value, from, to)
                else -> throw IllegalArgumentException("Unknown conversion type: $type")
            }
            """{"type": "$type", "value": $value, "from": "$from", "to": "$to", "result": $result}"""
        } catch (e: Exception) {
            """{"error": "${e.message}"}"""
        }
    }
    
    private fun convertLength(value: Double, from: String, to: String): Double {
        val meters = value * when (from.lowercase()) {
            "mm", "millimeter" -> 0.001
            "cm", "centimeter" -> 0.01
            "m", "meter" -> 1.0
            "km", "kilometer" -> 1000.0
            "in", "inch" -> 0.0254
            "ft", "foot" -> 0.3048
            "yd", "yard" -> 0.9144
            "mi", "mile" -> 1609.344
            else -> throw IllegalArgumentException("Unknown length unit: $from")
        }
        return meters / when (to.lowercase()) {
            "mm", "millimeter" -> 0.001
            "cm", "centimeter" -> 0.01
            "m", "meter" -> 1.0
            "km", "kilometer" -> 1000.0
            "in", "inch" -> 0.0254
            "ft", "foot" -> 0.3048
            "yd", "yard" -> 0.9144
            "mi", "mile" -> 1609.344
            else -> throw IllegalArgumentException("Unknown length unit: $to")
        }
    }
    
    private fun convertWeight(value: Double, from: String, to: String): Double {
        val kg = value * when (from.lowercase()) {
            "mg", "milligram" -> 0.000001
            "g", "gram" -> 0.001
            "kg", "kilogram" -> 1.0
            "oz", "ounce" -> 0.0283495
            "lb", "pound" -> 0.453592
            "t", "ton" -> 1000.0
            else -> throw IllegalArgumentException("Unknown weight unit: $from")
        }
        return kg / when (to.lowercase()) {
            "mg", "milligram" -> 0.000001
            "g", "gram" -> 0.001
            "kg", "kilogram" -> 1.0
            "oz", "ounce" -> 0.0283495
            "lb", "pound" -> 0.453592
            "t", "ton" -> 1000.0
            else -> throw IllegalArgumentException("Unknown weight unit: $to")
        }
    }
    
    private fun convertTemperature(value: Double, from: String, to: String): Double {
        val celsius = when (from.lowercase()) {
            "c", "celsius" -> value
            "f", "fahrenheit" -> (value - 32) * 5 / 9
            "k", "kelvin" -> value - 273.15
            else -> throw IllegalArgumentException("Unknown temperature unit: $from")
        }
        return when (to.lowercase()) {
            "c", "celsius" -> celsius
            "f", "fahrenheit" -> celsius * 9 / 5 + 32
            "k", "kelvin" -> celsius + 273.15
            else -> throw IllegalArgumentException("Unknown temperature unit: $to")
        }
    }
}

/**
 * Factory for creating tool instances
 */
object ToolFactory {
    fun createTool(name: String, context: Context, knowledgeRepository: com.mindflow.domain.repository.KnowledgeRepository? = null): Tool? {
        return when (name) {
            "web_search" -> WebSearchTool(context)
            "calculator" -> CalculatorTool()
            "text_summarizer" -> TextSummarizerTool()
            "knowledge_query" -> knowledgeRepository?.let { KnowledgeQueryTool(it) }
            "datetime" -> DateTimeTool()
            "url_fetch" -> UrlFetchTool(context)
            "converter" -> ConverterTool()
            else -> null
        }
    }
    
    fun getDefaultTools(): List<String> = listOf(
        "web_search",
        "calculator", 
        "text_summarizer",
        "datetime",
        "converter"
    )
}
