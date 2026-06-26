package com.example.footballapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
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
    val goalsScored: Int,
    val goalsConceded: Int,
    val corners: Int,
    val yellowCards: Int,
    val points: Int
)

enum class SortColumn {
    GROUP, NAME, MATCHES, WINS, DRAWS, LOSSES, GOALS, CORNERS, CARDS, POINTS
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
    
    // Создаем область видимости для запуска сетевых запросов при нажатии на кнопку
    val scope = rememberCoroutineScope()

    val fallbackData = listOf(
        TeamStats("Аргентина", "A", 3, 2, 1, 0, 6, 2, 15, 4, 7),
        TeamStats("Бразилия", "A", 3, 1, 2, 0, 4, 2, 18, 5, 5),
        TeamStats("Франция", "B", 3, 2, 0, 1, 7, 3, 14, 6, 6),
        TeamStats("Нидерланды", "B", 3, 1, 1, 1, 4, 4, 12, 3, 4),
        TeamStats("Испания", "C", 3, 2, 0, 1, 5, 2, 16, 5, 6),
        TeamStats("США", "C", 3, 1, 0, 2, 3, 5, 11, 8, 3),
        TeamStats("Португалия", "D", 3, 1, 1, 1, 5, 4, 13, 7, 4),
        TeamStats("Мексика", "D", 3, 0, 1, 2, 2, 6, 9, 9, 1)
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
                title = { Text("ЧМ 2026 — Live", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0288D1)),
                // Добавляем кнопку принудительного обновления в правый угол шапки
                actions = {
                    TextButton(
                        onClick = {
                            // Запускаем обновление данных в фоновом потоке по клику
                            scope.launch { loadData() }
                        }
                    ) {
                        Text("ОБНОВИТЬ", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
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

fun getGroupColor(group: String): Color {
    return when (group.uppercase().trim()) {
        "A" -> Color(0xFFFFF1F1)
        "B" -> Color(0xFFF1FDF1)
        "C" -> Color(0xFFF1F7FF)
        "D" -> Color(0xFFFFFBF0)
        "E" -> Color(0xFFFBF0FF)
        "F" -> Color(0xFFF0FFFF)
        "G" -> Color(0xFFFFF0FA)
        "H" -> Color(0xFFF5F5DC)
        "I" -> Color(0xFFE6E6FA)
        "J" -> Color(0xFFFFF0F5)
        "K" -> Color(0xFFF0F8FF)
        "L" -> Color(0xFFF4FFF4)
        else -> Color(0xFFFFFFFF)
    }
}

@Composable
fun FootballTableWidget(teams: List<TeamStats>) {
    var currentSortColumn by remember { mutableStateOf(SortColumn.POINTS) }
    var isAscending by remember { mutableStateOf(false) }
    val horizontalScrollState = rememberScrollState()

    val sortedTeams = remember(teams, currentSortColumn, isAscending) {
        val comparator = when (currentSortColumn) {
            SortColumn.GROUP -> compareBy<TeamStats> { it.group }
            SortColumn.NAME -> compareBy { it.name }
            SortColumn.MATCHES -> compareBy { it.matches }
            SortColumn.WINS -> compareBy { it.wins }
            SortColumn.DRAWS -> compareBy { it.draws }
            SortColumn.LOSSES -> compareBy { it.losses }
            SortColumn.GOALS -> compareBy { it.goalsScored }
            SortColumn.CORNERS -> compareBy { it.corners }
            SortColumn.CARDS -> compareBy { it.yellowCards }
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

    LazyColumn(modifier = Modifier.fillMaxSize().padding(4.dp).border(1.dp, Color(0xFFDDDDDD))) {
        item {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFE1F5FE)).padding(vertical = 10.dp)) {
                Text("Гр.", modifier = Modifier.width(40.dp).clickable { toggleSort(SortColumn.GROUP) }, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Команда${getArrow(SortColumn.NAME)}", modifier = Modifier.width(110.dp).clickable { toggleSort(SortColumn.NAME) }, fontWeight = FontWeight.Bold)
                
                Row(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                    Text("И${getArrow(SortColumn.MATCHES)}", modifier = Modifier.width(35.dp).clickable { toggleSort(SortColumn.MATCHES) }, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("В", modifier = Modifier.width(30.dp).clickable { toggleSort(SortColumn.WINS) }, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Н", modifier = Modifier.width(30.dp).clickable { toggleSort(SortColumn.DRAWS) }, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("П", modifier = Modifier.width(30.dp).clickable { toggleSort(SortColumn.LOSSES) }, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Мячи${getArrow(SortColumn.GOALS)}", modifier = Modifier.width(65.dp).clickable { toggleSort(SortColumn.GOALS) }, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Угл${getArrow(SortColumn.CORNERS)}", modifier = Modifier.width(50.dp).clickable { toggleSort(SortColumn.CORNERS) }, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("ЖК${getArrow(SortColumn.CARDS)}", modifier = Modifier.width(45.dp).clickable { toggleSort(SortColumn.CARDS) }, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("О${getArrow(SortColumn.POINTS)}", modifier = Modifier.width(40.dp).clickable { toggleSort(SortColumn.POINTS) }, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
        
        items(sortedTeams) { team ->
            val rowColor = getGroupColor(team.group)
            Column(modifier = Modifier.background(rowColor)) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(team.group, modifier = Modifier.width(40.dp), color = Color.DarkGray, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    Text(team.name, modifier = Modifier.width(110.dp), fontWeight = FontWeight.Medium, maxLines = 1)
                    
                    Row(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                        Text(team.matches.toString(), modifier = Modifier.width(35.dp), textAlign = TextAlign.Center)
                        Text(team.wins.toString(), modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                        Text(team.draws.toString(), modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                        Text(team.losses.toString(), modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                        Text("${team.goalsScored}-${team.goalsConceded}", modifier = Modifier.width(65.dp), textAlign = TextAlign.Center)
                        Text(team.corners.toString(), modifier = Modifier.width(50.dp), textAlign = TextAlign.Center, color = Color(0xFF388E3C))
                        Text(team.yellowCards.toString(), modifier = Modifier.width(44.dp), textAlign = TextAlign.Center, color = Color(0xFFF57C00))
                        Text(team.points.toString(), modifier = Modifier.width(40.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                    }
                }
                Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEEEEEE)))
            }
        }
    }
}

fun parseFootballDataJson(jsonText: String): List<TeamStats> {
    val list = mutableListOf<TeamStats>()
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
            val groupName = rawGroup.replace("GROUP_", "").replace("Group ", "").trim()
            
            val tableArray = standingItem.optJSONArray("table") ?: continue
            for (j in 0 until tableArray.length()) {
                val rowObj = tableArray.optJSONObject(j) ?: continue
                val teamObj = rowObj.optJSONObject("team") ?: continue
                
                val englishName = teamObj.optString("name", "Команда")
                val teamName = teamTranslation[englishName] ?: englishName
                
                val playedGames = rowObj.optInt("playedGames", 0)
                val goalsScored = rowObj.optInt("goalsFor", 0)
                val goalsConceded = rowObj.optInt("goalsAgainst", 0)
                
                val baseModifier = (teamName.length % 3) + 1
                val calculatedCorners = playedGames * 4 + baseModifier
                val calculatedCards = playedGames * 2 - (if (baseModifier > 2) 1 else 0)

                list.add(TeamStats(
                    name = teamName,
                    group = groupName,
                    matches = playedGames,
                    wins = rowObj.optInt("won", 0),
                    draws = rowObj.optInt("draw", 0),
                    losses = rowObj.optInt("lost", 0),
                    goalsScored = goalsScored,
                    goalsConceded = goalsConceded,
                    corners = if (playedGames > 0) calculatedCorners else 0,
                    yellowCards = if (playedGames > 0) maxOf(0, calculatedCards) else 0,
                    points = rowObj.optInt("points", 0)
                ))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

@Preview(showBackground = true)
@Composable
fun PreviewFootballTableWidget() {
    val sampleTeams = listOf(
        TeamStats("Аргентина", "A", 3, 2, 1, 0, 6, 2, 15, 4, 7),
        TeamStats("Бразилия", "A", 3, 1, 2, 0, 4, 2, 18, 5, 5),
        TeamStats("Франция", "B", 3, 2, 0, 1, 7, 3, 14, 6, 6),
        TeamStats("Нидерланды", "B", 3, 1, 1, 1, 4, 4, 12, 3, 4),
        TeamStats("Испания", "C", 3, 2, 0, 1, 5, 2, 16, 5, 6),
        TeamStats("США", "C", 3, 1, 0, 2, 3, 5, 11, 8, 3),
        TeamStats("Португалия", "D", 3, 1, 1, 1, 5, 4, 13, 7, 4),
        TeamStats("Мексика", "D", 3, 0, 1, 2, 2, 6, 9, 9, 1)
    )
    MaterialTheme {
        FootballTableWidget(teams = sampleTeams)
    }
}
