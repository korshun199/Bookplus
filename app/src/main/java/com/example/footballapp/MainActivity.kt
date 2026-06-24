package com.example.footballapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class TeamStats(
    val name: String,
    val group: String,
    val matches: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val points: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF9F9F9)) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var teamsList by remember { mutableStateOf(listOf<TeamStats>()) }
    val scope = rememberCoroutineScope()

    val loadData: suspend () -> Unit = {
        isLoading = true
        errorMessage = null
        try {
            val result = withContext(Dispatchers.IO) {
                val url = URL("https://worldcup26.ir/get/groups")
                val connection = url.openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    if (connection.responseCode == 200) {
                        val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                        parseTeamsJson(jsonText)
                    } else {
                        throw Exception("Код ответа сервера: ${connection.responseCode}")
                    }
                } finally {
                    connection.disconnect()
                }
            }
            teamsList = result.sortedByDescending { it.points }
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Ошибка сети"
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ЧМ 2026 — Таблицы", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0288D1))
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF0288D1))
            } else if (errorMessage != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ошибка загрузки данных", color = Color.Red, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { scope.launch { loadData() } }) { Text("Обновить") }
                }
            } else {
                FootballTableWidget(teams = teamsList)
            }
        }
    }
}

@Composable
fun FootballTableWidget(teams: List<TeamStats>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp).border(1.dp, Color(0xFFCCCCCC))) {
        item {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFE1F5FE)).padding(8.dp)) {
                Text("Гр.", modifier = Modifier.weight(0.12f), fontWeight = FontWeight.Bold)
                Text("Команда", modifier = Modifier.weight(0.38f), fontWeight = FontWeight.Bold)
                Text("И", modifier = Modifier.weight(0.1f), fontWeight = FontWeight.Bold)
                Text("В", modifier = Modifier.weight(0.1f), fontWeight = FontWeight.Bold)
                Text("Н", modifier = Modifier.weight(0.1f), fontWeight = FontWeight.Bold)
                Text("П", modifier = Modifier.weight(0.1f), fontWeight = FontWeight.Bold)
                Text("О", modifier = Modifier.weight(0.1f), fontWeight = FontWeight.Bold)
            }
        }
        items(teams) { team ->
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp).border(bottom = 1.dp, color = Color(0xFFF0F0F0))) {
                Text(team.group, modifier = Modifier.weight(0.12f), color = Color.Gray)
                Text(team.name, modifier = Modifier.weight(0.38f), fontWeight = FontWeight.Medium)
                Text(team.matches.toString(), modifier = Modifier.weight(0.1f))
                Text(team.wins.toString(), modifier = Modifier.weight(0.1f))
                Text(team.draws.toString(), modifier = Modifier.weight(0.1f))
                Text(team.losses.toString(), modifier = Modifier.weight(0.1f))
                Text(team.points.toString(), modifier = Modifier.weight(0.1f), fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
            }
        }
    }
}

fun parseTeamsJson(jsonText: String): List<TeamStats> {
    val list = mutableListOf<TeamStats>()
    try {
        if (jsonText.trim().startsWith("[")) {
            val groupsArray = JSONArray(jsonText)
            for (i in 0 until groupsArray.length()) {
                val groupObj = groupsArray.optJSONObject(i) ?: continue
                val groupName = groupObj.optString("group", "—")
                val teamsArray = groupObj.optJSONArray("teams") ?: continue
                for (j in 0 until teamsArray.length()) {
                    val teamObj = teamsArray.optJSONObject(j) ?: continue
                    list.add(TeamStats(
                        name = teamObj.optString("name", "Команда"),
                        group = groupName,
                        matches = teamObj.optInt("matches", 0),
                        wins = teamObj.optInt("wins", 0),
                        draws = teamObj.optInt("draws", 0),
                        losses = teamObj.optInt("losses", 0),
                        points = teamObj.optInt("points", 0)
                    ))
                }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return list
}
