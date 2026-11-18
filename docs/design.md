# Glassfolio design

## Class diagram

`+` public, `-` private, `#` protected. Hollow triangle = inheritance, dashed = implements an interface, diamond = has-a (composition).

```mermaid
classDiagram
    direction LR

    class Main {
        +main(String[] args)$
    }
    class StockApp {
        -Preferences preferences
        -ObjectProperty~Theme~ theme
        -Stage stage
        -FormScreen form
        -HistoryCache history
        -Market market
        -MarketConditions conditions
        +start(Stage stage)
        -suggest(User user)
        -loadEconomy() EconomySnapshot
        -loadTheme() Theme
    }
    class Theme {
        <<enumeration>>
        LIGHT
        DARK
        +stylesheet() String
        +shardColor() Color
        +topShardColor() Color
        +glow() boolean
        +other() Theme
    }
    class FormScreen {
        -ParticleChart chart
        +FormScreen(Consumer~User~ onSubmit, ObjectProperty~Theme~ theme)
        +show(Stage stage)
        +showBusy(boolean busy)
        +showError(String message)
        +showMarketPreview(List~Stock~ indexes, HistoryCache history, EconomySnapshot economy)
    }
    class ParticleChart {
        -Rotate turn
        -Rotate tilt
        -AnimationTimer timer
        +ParticleChart(Theme theme)
        +play()
        +setTheme(Theme theme)
    }
    class ResultsScreen {
        +ResultsScreen(String strategyName, List~Suggestion~ suggestions, List~Stock~ indexes, HistoryCache history, EconomySnapshot economy, ObjectProperty~Theme~ theme, Runnable onBack)
        +show(Stage stage)
    }
    class GlassTheme {
        <<utility>>
        +buildScene(Region content, ObjectProperty~Theme~ theme)$ Scene
        +applyTheme(Scene scene, ObservableValue~Theme~ theme)$
        +buildThemeSwitch(ObjectProperty~Theme~ theme)$ Button
    }
    class Styles {
        <<utility>>
        +String TITLE$
        +String SOLID_CARD$
        +String GLASS$
        +String PRIMARY_BUTTON$
        +String SHEET_URL$
    }
    class Motion {
        <<utility>>
        +playEntrance(Node node)$
        +addHoverPop(Button button)$
        +fadeInStaggered(Node node, int order)$
    }
    class MarketStrip {
        +MarketStrip()
        +node() Node
        +show(List~Stock~ indexes, HistoryCache history, EconomySnapshot economy)
    }
    class ProfileGuide {
        <<utility>>
        +build()$ VBox
    }
    class Sparkline {
        <<utility>>
        +build(List~Double~ values, boolean up, double width, double height)$ Pane
    }
    class Backdrop {
        <<utility>>
        +build()$ Pane
    }

    class User {
        +int STABILITY_PROFILE$
        +int GROWTH_PROFILE$
        -String username
        -String password
        -double budget
        -int riskProfile
        +getBudget() double
        +getRiskProfile() int
        +setBudget(double budget)
        +setRiskProfile(int riskProfile)
    }
    class Stratgey {
        <<abstract>>
        #String name
        +getName() String
        +adjustThreshold(double base, User user)* double
        +score(Stock stock, User user)* double
        +explain(Stock stock, User user)* String
    }
    class Stability {
        +double DEFAULT_MAX_VOLATILITY$
        -double baseMaxVolatility
    }
    class Growth {
        +double DEFAULT_MIN_GROWTH$
        -double baseMinGrowth
    }
    class Stock {
        -String ticker
        -String name
        -double price
        -double oneYearReturn
        -double volatility
        +fromPrices(String ticker, String name, List~Double~ prices)$ Stock
        +getOneYearReturn() double
        +getVolatility() double
    }
    class Suggestion {
        <<record>>
        +Stock stock
        +double score
        +String reason
        +double worth
        +rank(Stratgey strategy, User user, List~Stock~ indexes)$ List~Suggestion~
    }

    class MarketDataSource {
        <<interface>>
        +String LEVELS$
        +String PERCENT_CHANGE_FROM_YEAR_AGO$
        +getValues(String seriesId, LocalDate startDate, String units)* List~Double~
        +getValues(String seriesId, LocalDate startDate) List~Double~
    }
    class FredClient {
        -HttpClient http
        -String apiKey
        +getValues(String seriesId, LocalDate startDate, String units) List~Double~
        -buildUrl(String seriesId, LocalDate startDate, String units) URI
        -parseValues(byte[] xml) List~Double~
    }
    class HistoryCache {
        -MarketDataSource source
        -Map history
        +getValues(String seriesId, LocalDate startDate, String units) List~Double~
        +getPercentChange(String seriesId) List~Double~
    }
    class Market {
        -MarketDataSource source
        +loadIndexes() List~Stock~
    }
    class MarketConditions {
        -MarketDataSource source
        +load() EconomySnapshot
    }
    class EconomySnapshot {
        <<record>>
        +double fedFundsRate
        +double inflation
        +double unemployment
    }

    Main ..> StockApp : launches
    StockApp *-- FormScreen
    StockApp ..> ResultsScreen : creates
    StockApp *-- HistoryCache
    StockApp *-- Market
    StockApp *-- MarketConditions
    StockApp *-- Theme : current
    GlassTheme ..> Theme
    ParticleChart ..> Theme
    FormScreen *-- ParticleChart
    FormScreen *-- MarketStrip
    FormScreen ..> ProfileGuide
    MarketStrip ..> Sparkline
    ResultsScreen ..> Sparkline
    ProfileGuide ..> Stability
    ProfileGuide ..> Growth
    FormScreen ..> GlassTheme
    ResultsScreen ..> GlassTheme
    GlassTheme ..> Backdrop
    GlassTheme ..> Styles
    FormScreen ..> Styles
    ResultsScreen ..> Styles
    FormScreen ..> Motion
    ResultsScreen ..> Motion
    ResultsScreen ..> Suggestion
    Stratgey <|-- Stability
    Stratgey <|-- Growth
    MarketDataSource <|.. FredClient
    MarketDataSource <|.. HistoryCache
    HistoryCache o-- MarketDataSource : wraps
    Market o-- MarketDataSource
    MarketConditions o-- MarketDataSource
    Market ..> Stock : creates
    MarketConditions ..> EconomySnapshot : creates
    Suggestion --> Stock
    Suggestion ..> Stratgey : uses
```

### Design patterns used

| Pattern | Where | Why |
|---|---|---|
| Strategy | `Stratgey`, `Stability`, `Growth` | Each risk profile scores indexes its own way; callers only use `score()` and `explain()`. |
| Decorator | `HistoryCache` | Same `MarketDataSource` interface as `FredClient`, adds remembering each series for the charts. |
| Builder | `FredClient.buildUrl` (`StringBuilder`) | Builds the request URL step by step. |
| Factory method | `Stock.fromPrices` | Builds a `Stock` from raw prices so callers don't do the math. |
| MVC | Model: logic classes. View: `FormScreen`, `ResultsScreen`. Controller: `StockApp` | Screens only display; rules live in the model. |
| Observer | Button and `Task` event listeners; screens listen to the shared `theme` property | Event-driven GUI; switching the theme restyles every open screen at once. |
| Dependency injection | `StockApp` builds everything and passes it down | `Market` and `MarketConditions` work with any data source, including test fakes. |

## Sequence diagram: getting suggestions

```mermaid
sequenceDiagram
    actor U as User
    participant F as FormScreen
    participant A as StockApp
    participant T as Task (background thread)
    participant M as Market
    participant C as MarketConditions
    participant H as HistoryCache
    participant R as FredClient
    participant API as FRED API
    participant S as Suggestion
    participant RS as ResultsScreen

    U->>F: Enter name, budget, risk profile, click Get suggestions
    F->>F: Validate input
    F->>A: onSubmit(user)
    A->>A: Pick Stability or Growth
    A->>F: showBusy(true)
    A->>T: start
    T->>M: loadIndexes()
    loop SP500, NASDAQCOM, DJIA
        M->>H: getValues(seriesId, oneYearAgo, LEVELS)
        H->>R: getValues(seriesId, oneYearAgo, LEVELS)
        R->>API: GET /fred/series/observations
        API-->>R: XML observations
        R-->>H: prices
        H-->>M: prices (kept for charts)
        M->>M: Stock.fromPrices()
    end
    M-->>T: List<Stock>
    T->>C: load()
    C->>H: FEDFUNDS, UNRATE, CPIAUCSL (year-over-year %)
    H-->>C: values
    C-->>T: EconomySnapshot
    T-->>A: succeeded
    A->>F: showBusy(false)
    A->>S: rank(strategy, user, indexes)
    S-->>A: suggestions, best first
    A->>RS: new ResultsScreen(...).show(stage)
    RS-->>U: Economy tiles, ranked cards, past-year chart
```

If loading fails (bad key, no internet), the task's `onFailed` shows the error on the form instead.

## Window sizes

Both screens stretch to fill the window, from the 860×640 minimum up to full screen (F11). The 3D chart scales with its area's height, the background blobs and stars keep their places relative to the window, and the results page caps its width at 1280 px so lines stay easy to read, with the chart growing into any spare height.

Glassfolio is a desktop JavaFX app. It runs on Windows, macOS and Linux, not on phones.

## Themes

Every style refers to named colors (`-text`, `-accent`, `-surface` and so on). `Theme` defines them once for light and once for dark, so switching only swaps one small stylesheet. The 3D shards can't use CSS, so `Theme` also holds their colors and whether they glow. The choice is saved with Java's `Preferences`.

| Light | Dark |
|---|---|
| ![Light home](storyboard/light-form.png) | ![Dark home](storyboard/1-form.png) |
| ![Light results](storyboard/light-results.png) | ![Dark results](storyboard/2-results.png) |

## Storyboard

Screenshots use made-up sample data, not live FRED numbers, and the dark theme.

| Step | Screen |
|---|---|
| 1. Glowing shards assemble into a rising chart while the user fills in the form. A live market strip and a profile guide fill the rest of the screen. | ![Form](storyboard/1-form.png) |
| 2. Glassfolio loads FRED data and ranks the indexes. The best pick is highlighted. | ![Results](storyboard/2-results.png) |
| 3. If no index fits the profile, the app says so and still shows the chart. | ![No match](storyboard/3-no-match.png) |
| 4. If the data can't load (for example a bad API key), the form shows why. | ![Error](storyboard/4-error.png) |
