# LexDistance — Lexical Distance Analyzer (MVC + JavaFX + Concurrency)

A compact, teaching-oriented Java 23 + JavaFX app to **compute and visualize lexical distance across languages**. It imports dictionaries, normalizes strings (Unicode **NFC**), computes a full **distance matrix** with **Levenshtein**, **Damerau–Levenshtein**, **LCS**, and **Jaro–Winkler**, and renders an **interactive table**, a **similarity graph**, and a **UPGMA dendrogram**—all under a clean **MVC** design with a decoupled notification bus.

> Parallelized with an Executor/Fork-Join strategy: on quad-core hardware we observed up to **\~42% runtime reduction** vs. the sequential build. UI stays responsive via async notifications marshaled onto the JavaFX thread.

---

## Features

* **Multi-metric distance engine**: Levenshtein, Damerau–Levenshtein, Longest Common Subsequence (LCS), and Jaro–Winkler, each with clear algorithmic contracts.
* **Smart data ingest**: load dictionaries from **.dic / .txt / .csv**, normalize to **Unicode NFC** for consistent comparisons.
* **Visual analytics**: interactive **distance matrix**, **similarity graph**, and **UPGMA dendrogram** for phylo-lexical structure.
* **Fast & responsive**: fork/join partitioning of the distance matrix build; progress events stream to the UI without blocking.
* **Extensible by design**: `AlgorithmFactory` + `AlgorithmType` make it trivial to add new distance metrics.

---

## Architecture (MVC + events)

```
View (JavaFX)
  ├─ LexView                    ← main UI
  ├─ DendogramaView            ← dendrogram renderer
  └─ PolygeneticGraphView      ← similarity graph
        ↑ UI-safe updates via NotificationService
Controller
  └─ LexController             ← validates inputs, orchestrates runs, handles events
        ↑ subscribes to notifications
        ↓ dispatches background work / updates view
Model
  ├─ LexModel                  ← core: algorithms, distance matrix, event publication
  ├─ dictionary/               ← loaders for .dic/.txt/.csv → Word objects
  ├─ DistanceAlgorithm         ← common contract
  ├─ AlgorithmFactory / AlgorithmType
  └─ DistanceMatrix            ← storage/derivations (graph, dendrogram)
Infra
  ├─ NotificationService       ← pub/sub interface
  └─ NotificationServiceImpl   ← event bus + Platform.runLater handoff
```

This event-driven design decouples long-running computation from the UI thread; all visual changes are marshaled with `Platform.runLater` to keep rendering smooth.

---

## Algorithms & complexity (at a glance)

* **Levenshtein** — DP (Wagner–Fischer): **O(n·m)** time; space **O(n·m)** (or **O(min(n,m))** with row reuse). Thresholding/early exit supported.
* **Damerau–Levenshtein** — Levenshtein + adjacent transposition case; **O(n·m)** time; needs extra row history.
* **LCS** — DP table, **O(n·m)** time; Hirschberg reduces space to **O(n+m)** for reconstruction.
* **Jaro–Winkler** — windowed matching + transpositions + prefix boost; **O(n·m)** worst-case, **O(n+m)** aux space. Best for short names/strings.

---

## Concurrency model

* **Matrix build = parallel job**: split the upper triangle into balanced **fork/join** subtasks (per-row/row-block). Each task writes disjoint blocks → trivial thread safety. Progress tracked via an `AtomicInteger`.
* **Thread pools tuned to hardware**: core pool scales with available processors; small tasks flip to sequential below a dynamic threshold to avoid oversplitting.
* **UI notifications**: the Model publishes events; the Controller updates the View on the JavaFX Application Thread.

---

## Using the app

1. **Load dictionaries** (`.dic`, `.txt`, `.csv`). Strings are normalized to **NFC**.
2. **Pick algorithm** (Levenshtein, Damerau–Levenshtein, LCS, Jaro–Winkler).
3. **Compute**: watch progress; explore **matrix**, **graph**, and **dendrogram** outputs.

---

## Key components (what they do)

| Component                            | Role                                                                                                         |
| ------------------------------------ | ------------------------------------------------------------------------------------------------------------ |
| `LexModel`                           | Core model: runs selected metric, builds the distance matrix, emits events; derives graph/dendrogram views.  |
| `DistanceAlgorithm`                  | Interface for all metrics (`calculateDistance`, normalized variants, metadata).                              |
| `AlgorithmFactory` / `AlgorithmType` | Factory Method + enum mapping selects metric at runtime; easy to extend.                                     |
| `dictionary/*` + `Word`              | Robust loaders for `.dic/.txt/.csv` → normalized tokens.                                                     |
| `LexController`                      | Bridges View↔Model; validates inputs; handles and dispatches events.                                         |
| `LexView`                            | Main JavaFX UI (menus, controls, matrix table, graph/dendrogram panes).                                      |
| `DendogramaView`                     | UPGMA dendrogram renderer.                                                                                   |
| `PolygeneticGraphView`               | Similarity graph with interactive exploration.                                                               |
| `NotificationService(Impl)`          | Pub/sub event bus; marshals updates to the JavaFX thread.                                                    |

---

## Further reading

The project report (Catalan) details **design choices**, **algorithms**, **concurrency tuning**, and the **event flow** with sequence diagrams.

---


## Credits

**Dylan Canning Garcia** and collaborators (see report). Thanks to the course staff for guidance, code reviews, and evaluation.

---

### Citation (if you use this in teaching/research)

> Canning Garcia, D., et al. *LexDistance — Lexical Distance Analyzer (MVC + JavaFX + Concurrency).* Project code and report, 2025.
