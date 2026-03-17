# Test Distribution Extension for Paparazzi

Compatibility library and Gradle plugin to use [Paparazzi](https://github.com/cashapp/paparazzi) with [Develocity Test Distribution](https://docs.gradle.com/develocity/current/administration/admin-manual/#test_distribution).

## The problem

Test Distribution distributes test execution across remote agents. When tests finish,
it merges outputs from each agent. Paparazzi generates a single HTML report per test run,
causing conflicts when multiple agents write to the same output directory:

```
Test sessions created conflicting fragments in output directory
'example/internal/build/reports/paparazzi' for 'index.html', 'index.js', 'paparazzi.js'
```

## The solution

This extension generates individual report directories per test execution (`td-<timestamp>/`),
avoiding fragment conflicts. A Gradle plugin handles merging the individual reports into
a single consolidated report after all tests complete.

```
build/reports/paparazzi/
  td-1684883866433/
    images/
    runs/
    videos/
  td-1684883867241/
    images/
    runs/
    videos/
```

## Usage

### Option 1: Gradle Plugin (recommended)

Apply the plugin alongside Paparazzi. It automatically registers the merge task and wires
everything up per variant:

```kotlin
// build.gradle.kts
plugins {
    id("com.android.library")
    id("app.cash.paparazzi")
    id("io.github.cdsap.td.paparazzi")
}
```

In your tests, use `TDPaparazzi()` as a drop-in replacement for `Paparazzi()`:

```kotlin
import io.github.cdsap.td.paparazzi.TDPaparazzi

class MyScreenTest {
    @get:Rule
    val paparazzi = TDPaparazzi(maxPercentDifference = 0.1)

    @Test
    fun snapshot() {
        paparazzi.snapshot { MyComposable() }
    }
}
```

`TDPaparazzi()` accepts all the same parameters as `Paparazzi()` (`deviceConfig`, `theme`,
`renderingMode`, `appCompatEnabled`, `renderExtensions`, `supportsRtl`, `showSystemUi`,
`useDeviceResolution`).

The plugin can be configured via the `tdPaparazzi` extension:

```kotlin
tdPaparazzi {
    inputReportDir.set("build/reports/paparazzi")   // default
    outputReportDir.set("build/reports/paparazzi-td") // default
    libraryVersion.set("0.2.0")                         // default
}
```

### Option 2: Manual setup

Add the library dependency:

```kotlin
testImplementation("io.github.cdsap:td-paparazzi-ext:0.2.0")
```

Use `TDPaparazzi()` in your tests (same as above), then configure Test Distribution
inputs/outputs and define the merge task manually:

```kotlin
tasks.withType<Test>().configureEach {
    inputs.file("build/intermediates/paparazzi/${
        name.replace("UnitTest", "").replace("test", "").lowercase()
    }/resources.txt")
    outputs.dir("build/reports/paparazzi-td/")
    outputs.dir("out/failures/")
    distribution {
        // ...
    }
}
```

## Sample

The `android-library` module in this repository contains a complete working example:
- [build.gradle.kts](https://github.com/cdsap/PaparazziTestDistributionExtension/blob/main/android-library/build.gradle.kts) — plugin applied, no manual merge task
- [ExampleUnitTest.kt](https://github.com/cdsap/PaparazziTestDistributionExtension/blob/main/android-library/src/test/java/com/example/myapplication/ExampleUnitTest.kt) — test using `TDPaparazzi()`

## Output

### Individual reports

![](resources/individualreport.png)

### Merged reports

![](resources/mergedreport.png)

### Example output after merging the individual reports

![](resources/outputwithtd.png)

## Project structure

| Module | Description |
|--------|-------------|
| `lib` | Core library — `TDHtmlReportWriter`, `TDPaparazzi` factory, JSON serialization |
| `gradle-plugin` | Gradle plugin — auto-registers merge tasks, adds library dependency |
| `android-library` | Sample Android module demonstrating the extension |

## Notes

The configuration is tested for Paparazzi executions of one build variant.
