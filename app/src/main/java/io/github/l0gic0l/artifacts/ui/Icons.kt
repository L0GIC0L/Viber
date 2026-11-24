package io.github.l0gic0l.artifacts.ui

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** A few Material Symbols not in material-icons-core (avoids the huge extended set). */
object AppIcons {
    val Download = icon("Download", "M5,20h14v-2H5V20zM19,9h-4V3H9v6H5l7,7L19,9z")
    val Package = icon(
        "Package",
        "M20,6h-8l-2,-2H4C2.9,4 2.01,4.9 2.01,6L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8C22,6.9 21.1,6 20,6zM20,18H4V8h16V18z",
    )
    val InstallMobile = icon(
        "InstallMobile",
        "M17,1H7C5.9,1 5,1.9 5,3v18c0,1.1 0.9,2 2,2h10c1.1,0 2,-0.9 2,-2V3C19,1.9 18.1,1 17,1zM17,18H7V6h10V18zM16,12h-3V8h-2v4H8l4,4L16,12z",
    )
    val Key = icon(
        "Key",
        "M12.65,10C11.83,7.67 9.61,6 7,6c-3.31,0 -6,2.69 -6,6s2.69,6 6,6c2.61,0 4.83,-1.67 5.65,-4H17v4h4v-4h2v-4H12.65zM7,14c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2 2,0.9 2,2 -0.9,2 -2,2z",
    )
    val Branch = icon(
        "Branch",
        "M14,4l2.29,2.29 -2.88,2.88 1.42,1.42 2.88,-2.88L20,10V4zM10,4H4v6l2.29,-2.29 4.71,4.7V20h2v-8.41l-5.29,-5.3z",
    )

    private fun icon(name: String, path: String) = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black)).build()
}
