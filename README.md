
# 📈 Symulator Inwestycyjny

**Projekt**: Graficzny symulator inwestycji napisany w Javie (JavaFX + Maven).

---

## 🎯 Opis

Symulator pozwala użytkownikowi:
- Wybierać tryb działania (manualny lub automatyczny) przy starcie.
- Zarządzać wirtualnym portfelem aktywów (akcje, obligacje, kryptowaluty).
- Kupować i sprzedawać aktywa, obserwując ich zmieniającą się cenę w czasie.
- Śledzić historię cen na wykresie liniowym.
- Zobaczyć wskazówki w tabeli (cena zakupu vs. aktualna cena) z symbolami strzałek (↑, ↓).

---

## 🚀 Funkcje

1. **Wybór trybu działania**:  
   - Manualny – użytkownik sam wykonuje kroki symulacji.  
   - Automatyczny – aplikacja symuluje rynek w pętli (tryb planowany).

2. **Obsługa portfela**:  
   - Dodawanie nowych aktywów do portfela.  
   - Sprzedaż posiadanych aktywów.  
   - Automatyczne obliczanie średniej ceny zakupu.  
   - Wyświetlanie wartości portfela i dostępnego salda.

3. **Symulacja rynku**:  
   - Dynamiczne zmiany cen aktywów na podstawie parametru `volatility`.  
   - Wykres liniowy pokazujący historię zmian ceny (oznaczenia kroku symulacji „timeStep”).

4. **Interfejs graficzny (JavaFX)**:  
   - Przyjazne GUI z widokiem tabeli (`TableView`) i wykresem (`LineChart`).  
   - Konfigurowalne kolumny tabeli z kolorami i strzałkami, pokazujące czy cena rośnie, spada, czy utrzymuje się.

---

## 📂 Struktura projektu

```
Symulator/
├─ pom.xml
├─ mvnw, mvnw.cmd
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  ├─ com/pans/konrad/apka/
│  │  │  │  ├─ logic/MarketService.java
│  │  │  │  ├─ model/
│  │  │  │  │  ├─ Asset.java
│  │  │  │  │  ├─ AssetType.java
│  │  │  │  │  └─ PortfolioEntry.java
│  │  │  │  └─ ui/
│  │  │  │     ├─ AssetController.java
│  │  │  │     ├─ ChoiceController.java
│  │  │  │     ├─ InvestmentSimulatorApp.java
│  │  │  │     ├─ MainController.java
│  │  │  │     └─ TableUtil.java
│  │  └─ resources/
│  │     ├─ com/pans/konrad/apka/AssetWindow.fxml
│  │     ├─ com/pans/konrad/apka/choice.fxml
│  │     ├─ com/pans/konrad/apka/layout.fxml
│  │     └─ com/pans/konrad/apka/style.css
├─ .gitignore
└─ README.md
```

---

## ⚙️ Wymagania

- Java 17 (OpenJDK 17 lub nowsze)
- Maven (wersja 3.x)
- Środowisko z GUI (uruchomienie aplikacji JavaFX wymaga środowiska graficznego)

---

## 💡 Użycie

1. **Ekran wyboru trybu** (`choice.fxml`):  
   - Wpisz kapitał początkowy (domyślnie 10 000).  
   - Wybierz „Manual Mode” (ręczna symulacja) lub „Auto Mode” (planowana symulacja).

2. **Główne okno symulacji** (`layout.fxml`):  
   - **Tabela portfela**:  
     - Kolumny: nazwa aktywa, ilość, średnia cena zakupu (ze strzałkami).  
   - **Wykres historii cen**:  
     - Wyświetla kolejne wartości ceny wybranego aktywa.  
   - **Przyciski**:  
     - **Buy**: kupuje wybraną ilość aktywów.  
     - **Sell**: sprzedaje.  
     - **Next Step**: przejście do kolejnego kroku symulacji (zmiana cen).  
   - **Pola tekstowe**:  
     - Wpisz nazwę aktywa i ilość, którą chcesz kupić/sprzedać.  

---

## 📄 Licencja

Projekt udostępniony na licencji [MIT](https://opensource.org/licenses/MIT).
