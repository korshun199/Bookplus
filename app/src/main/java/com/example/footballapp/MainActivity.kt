package com.example.footballapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import org.json.JSONObject
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

enum class SortColumn {
    GROUP, NAME, MATCHES, WINS, DRAWS, LOSSES, POINTS
}

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
    var teamsList by remember { mutableStateOf(listOf<TeamStats>()) }

    val fallbackData = listOf(
        TeamStats("Аргентина", "A", 3, 2, 1, 0, 7),
        TeamStats("Франция", "B", 3, 2, 0, 1, 6),
        TeamStats("Испания", "C", 3, 2, 0, 1, 6),
        TeamStats("Бразилия", "A", 3, 1, 2, 0, 5),
        TeamStats("Португалия", "D", 3, 1, 1, 1, 4),
        TeamStats("Нидерланды", "B", 3, 1, 1, 1, 4),
        TeamStats("США", "C", 3, 1, 0, 2, 3),
        TeamStats("Мексика", "D", 3, 0, 1, 2, 1)
    )

    val loadData: suspend () -> Unit = {
        isLoading = true
        try {
            val result = withContext(Dispatchers.IO) {
                val url = URL("https://api.football-data.org/v4/competitions/WC/standings")
                val connection = url.openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("X-Auth-Token", "e4cb8b2594414c0aaa62ccff4f48ed89")
                    
                    if (connection.responseCode == 200) {
                        val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                        parseFootballDataJson(jsonText)
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    emptyList()
                } finally {
                    connection.disconnect()
                }
            }

            teamsList = if (result.isEmpty()) fallbackData else result
            isLoading = false
        } catch (e: Exception) {
            teamsList = fallbackData
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ЧМ 2026 — Интерактивная таблица", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0288D1))
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF0288D1))
            } else {
                FootballTableWidget(teams = teamsList)
            }
        }
    }
}

@Composable
fun FootballTableWidget(teams: List<TeamStats>) {
    var currentSortColumn by remember { mutableStateOf(SortColumn.POINTS) }
    var isAscending by remember { mutableStateOf(false) }

    // Логика динамической сортировки данных
    val sortedTeams = remember(teams, currentSortColumn, isAscending) {
        val comparator = when (currentSortColumn) {
            SortColumn.GROUP -> compareBy<TeamStats> { it.group }
            SortColumn.NAME -> compareBy { it.name }
            SortColumn.MATCHES -> compareBy { it.matches }
            SortColumn.WINS -> compareBy { it.wins }
            SortColumn.DRAWS -> compareBy { it.draws }
            SortColumn.LOSSES -> compareBy { it.losses }
            SortColumn.POINTS -> compareBy { it.points }
        }
        if (isAscending) teams.sortedWith(comparator) else teams.sortedWith(comparator).reversed()
    }

    val toggleSort: (SortColumn) -> Unit = { column ->
        if (currentSortColumn == column) {
            isAscending = !isAscending
        } else {
            currentSortColumn = column
            isAscending = (column == SortColumn.NAME || column == SortColumn.GROUP)
        }
    }

    val getArrow: (SortColumn) -> String = { column ->
        if (currentSortColumn == column) (if (isAscending) "▲" else "▼") else ""
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp).border(1.dp, Color(0xFFCCCCCC))) {
        item {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFE1F5FE)).padding(8.dp)) {
                Text(text = "Гр.${getArrow(SortColumn.GROUP)}", modifier = Modifier.weight(0.12f).clickable { toggleSort(SortColumn.GROUP) }, fontWeight = FontWeight.Bold)
                Text(text = "Команда${getArrow(SortColumn.NAME)}", modifier = Modifier.weight(0.38f).clickable { toggleSort(SortColumn.NAME) }, fontWeight = FontWeight.Bold)
                Text(text = "И${getArrow(SortColumn.MATCHES)}", modifier = Modifier.weight(0.1f).clickable { toggleSort(SortColumn.MATCHES) }, fontWeight = FontWeight.Bold)
                Text(text = "В${getArrow(SortColumn.WINS)}", modifier = Modifier.weight(0.1f).clickable { toggleSort(SortColumn.WINS) }, fontWeight = FontWeight.Bold)
                Text(text = "Н${getArrow(SortColumn.DRAWS)}", modifier = Modifier.weight(0.1f).clickable { toggleSort(SortColumn.DRAWS) }, fontWeight = FontWeight.Bold)
                Text(text = "П${getArrow(SortColumn.LOSSES)}", modifier = Modifier.weight(0.1f).clickable { toggleSort(SortColumn.LOSSES) }, fontWeight = FontWeight.Bold)
                Text(text = "О${getArrow(SortColumn.POINTS)}", modifier = Modifier.weight(0.1f).clickable { toggleSort(SortColumn.POINTS) }, fontWeight = FontWeight.Bold)
            }
        }
        items(sortedTeams) { team ->
            Column {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(team.group, modifier = Modifier.weight(0.12f), color = Color.Gray)
                    Text(team.name, modifier = Modifier.weight(0.38f), fontWeight = FontWeight.Medium)
                    Text(team.matches.toString(), modifier = Modifier.weight(0.1f))
                    Text(team.wins.toString(), modifier = Modifier.weight(0.1f))
                    Text(team.draws.toString(), modifier = Modifier.weight(0.1f))
                    Text(team.losses.toString(), modifier = Modifier.weight(0.1f))
                    Text(team.points.toString(), modifier = Modifier.weight(0.1f), fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                }
                Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF0F0F0)))
            }
        }
    }
}

fun parseFootballDataJson(jsonText: String): List<TeamStats> {
    val list = mutableListOf<TeamStats>()
    
    // Словарь-переводчик англоязычных названий стран участников ЧМ на русский язык
    val teamTranslation = mapOf(
        "Argentina" to "Аргентина", "France" to "Франция", "Spain" to "Испания",
        "Brazil" to "Бразилия", "Portugal" to "Португалия", "Netherlands" to "Нидерланды",
        "USA" to "США", "Mexico" to "Мексика", "Germany" to "Германия",
        "England" to "Англия", "Italy" to "Италия", "Croatia" to "Хорватия",
        "Morocco" to "Марокко", "Japan" to "Япония", "South Korea" to "Южная Корея",
        "Uruguay" to "Уругвай", "Senegal" to "Сенегал", "Canada" to "Канада",
        "Iran" to "Иран", "Saudi Arabia" to "Саудовская Аравия", "Belgium" to "Бельгия",
        "Switzerland" to "Швейцария", "Denmark" to "Дания", "Tunisia" to "Тунис",
        "Poland" to "Польша", "Australia" to "Австралия", "Ecuador" to "Эквадор", 
        "Qatar" to "Катар", "Wales" to "Уэльс", "Costa Rica" to "Коста-Рика", 
        "Cameroon" to "Камерун", "Ghana" to "Гана", "Serbia" to "Сербия"
    )

    try {
        val rootObj = JSONObject(jsonText)
        val standingsArray = rootObj.optJSONArray("standings") ?: return list
        
        for (i in 0 until standingsArray.length()) {
            val standingItem = standingsArray.optJSONObject(i) ?: continue
            val rawGroup = standingItem.optString("group", "—")
            val groupName = rawGroup.replace("GROUP_", "")
            
            val tableArray = standingItem.optJSONArray("table") ?: continue
            for (j in 0 until tableArray.length()) {
                val rowObj = tableArray.optJSONObject(j) ?: continue
                val teamObj = rowObj.optJSONObject("team") ?: continue
                
                val englishName = teamObj.optString("name", "Команда")
                // Если страна есть в нашем словаре — берем русский перевод, иначе оставляем оригинал
                val teamName = teamTranslation[englishName] ?: englishName
                
                list.add(TeamStats(
                    name = teamName,
                    group = groupName,
                    matches = rowObj.optInt("playedGames", 0),
                    wins = rowObj.optInt("won", 0),
                    draws = rowObj.optInt("draw", 0),
                    losses = rowObj.optInt("lost", 0),
                    points = rowObj.optInt("points", 0)
                ))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}
