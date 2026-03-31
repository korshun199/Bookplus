import os
import email
from bs4 import BeautifulSoup
from rich.console import Console
from rich.table import Table

console = Console()

def get_clean_html(file_path):
    """
    Распаковывает MHTML и извлекает основной HTML-контент,
    игнорируя картинки, стили и мусор.
    """
    if not os.path.exists(file_path):
        return None

    with open(file_path, 'rb') as f:
        # MHTML по структуре похож на электронное письмо (multipart)
        msg = email.message_from_binary_file(f)
    
    # Ищем часть, которая содержит HTML-код страницы
    for part in msg.walk():
        if part.get_content_type() == "text/html":
            payload = part.get_payload(decode=True)
            # Декодируем, пропуская битые символы
            return payload.decode('utf-8', errors='ignore')
    
    # Если это не MHTML, а обычный файл, пробуем прочитать его просто так
    try:
        with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
            return f.read()
    except:
        return None

def parse_ligastavok_html(file_path):
    console.print(f"[bold cyan]🔍 Анализируем файл:[/bold cyan] [white]{file_path}[/white]")
    
    html_content = get_clean_html(file_path)
    if not html_content:
        console.print("[bold red]❌ Ошибка: Не удалось извлечь HTML из файла.[/bold red]")
        return

    # Используем стандартный html.parser, чтобы не зависеть от lxml
    soup = BeautifulSoup(html_content, "html.parser")

    # 1. Извлекаем названия команд
    # Классы могут немного меняться, поэтому ищем по вхождению имени
    teams = soup.find_all("div", class_=lambda x: x and "team-info-content__name" in x)
    team1 = teams[0].get_text(strip=True) if len(teams) > 0 else "Команда 1"
    team2 = teams[1].get_text(strip=True) if len(teams) > 1 else "Команда 2"

    # 2. Извлекаем дату и время
    date_val = soup.find("div", {"data-t-id": "event-start-date"})
    time_val = soup.find("div", {"data-t-id": "event-start-time"})
    date_str = date_val.get_text(strip=True) if date_val else "---"
    time_str = time_val.get_text(strip=True) if time_val else "---"

    console.print(f"\n[bold green]⚽ Матч:[/bold green] [yellow]{team1} — {team2}[/yellow]")
    console.print(f"[bold blue]📅 Начало:[/bold blue] {date_str} в {time_str}\n")

    # 3. Создаем таблицу для вывода коэффициентов
    table = Table(title="Линия БК Лига Ставок", header_style="bold magenta", box=None)
    table.add_column("Рынок (Маркет)", style="cyan", width=25)
    table.add_column("Исход", style="white", width=15)
    table.add_column("Коэф.", style="bold green", justify="right")

    # 4. Ищем блоки маркеров (Победитель, Тотал и т.д.)
    # Сайт использует атрибут data-t-market для заголовков рынков
    markets = soup.find_all("article", attrs={"data-t-market": True})
    
    found_any = False
    for market in markets:
        market_name = market['data-t-market']
        
        # Ищем кнопки со ставками внутри блока
        buttons = market.find_all("button", attrs={"outcomeid": True})
        
        for btn in buttons:
            # Название исхода (1, X, 2, Больше, Меньше)
            # Обычно лежит в div с классом, содержащим 'color-icotex-low'
            outcome_label = btn.find("div", class_=lambda x: x and "color-icotex-low" in x)
            # Само число коэффициента
            odds_val = btn.find("span", class_=lambda x: x and "text-headings-16-bold" in x)
            
            if outcome_label and odds_val:
                table.add_row(
                    market_name, 
                    outcome_label.get_text(strip=True), 
                    odds_val.get_text(strip=True)
                )
                found_any = True

    if found_any:
        console.print(table)
    else:
        console.print("[yellow]⚠️ Коэффициенты не найдены. Возможно, страница еще подгружается или формат изменился.[/yellow]")

if __name__ == "__main__":
    # Сюда подставь имя своего файла
    FILE_NAME = "match.html"
    parse_ligastavok_html(FILE_NAME)