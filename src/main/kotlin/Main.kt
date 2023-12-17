import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlin.math.abs


@OptIn(ExperimentalTextApi::class, ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App() {
    var txtMeasurer = rememberTextMeasurer()

    var xMin by remember { mutableStateOf(-10f) }
    var yMin by remember { mutableStateOf(0f) }
    var xMax by remember { mutableStateOf(10f) }
    var yMax by remember { mutableStateOf(10f) }

    var yMinMax by remember { mutableStateOf(0f) }
    var selectY by remember { mutableStateOf(true) }
    var points by remember { mutableStateOf(mutableMapOf<Float,Float>()) }
    var pointsD by remember { mutableStateOf(mutableMapOf<Float, Float>()) }

    Canvas(modifier = Modifier.fillMaxSize().clickable{}.
    onPointerEvent(PointerEventType.Press)
    {
        var scale = Scales(this.size.width, this.size.height,
            xMin, xMax, yMin, yMax)

        // Нажатые точки в экранных координатах
        var point = it.changes.first().position
        points[point.x] = point.y

        // Нажатые точки в декартовых координатах (эти точки нужны для изменения графика вселд за изменением размера окна)
        var s = ScreenPoints(point.x, point.y)
        var d = Decarts(0f, 0f)
        d.toDecarts(s, scale)
        pointsD[d.x] = d.y

    },
        onDraw = {
            var scale = Scales(this.size.width.toInt(), this.size.height.toInt(),
                xMin, xMax, yMin, yMax)

            var yMax = this.size.height*(xMax-xMin)/this.size.width + yMin
            if (yMax*yMin < 0){
                yMinMax = ((xMax-xMin) / (xMax+xMin)) // Некий коэффициент, нужен для слайдера
            }
            //Ox
            drawLine(
                color = Color.Black,
                start = Offset(0f, scale.h*(1+yMinMax)/2),
                end = Offset(scale.w.toFloat(), scale.h*(1+yMinMax)/2)
            )
            //Oy
            drawLine(
                color = Color.Black,
                start = Offset(-scale.w * xMin/(xMax-xMin), 0f),
                end = Offset(scale.w*abs(xMin)/(xMax-xMin), scale.h.toFloat())
            )

            // Отрисовка цифр на осях + разметка
            for(i in xMin.toInt()..xMax.toInt()){
                drawText(
                    textMeasurer = txtMeasurer,
                    text = i.toString(),
                    topLeft = Offset(scale.w*(i-xMin)/(xMax-xMin)-5, scale.h*(1+yMinMax)/2)
                )

                drawLine(
                    color = Color.Black,
                    start = Offset(scale.w*(i-xMin)/(xMax-xMin), scale.h*(1+yMinMax)/2-5f),
                    end = Offset(scale.w*(i-xMin)/(xMax-xMin), scale.h*(1+yMinMax)/2+5f)
                )
            }

            // Отрисовка точек
            for(point in pointsD){
                var s = ScreenPoints(0f,0f)
                var d = Decarts(point.key,point.value)
                s.toScreenPoint(d, scale)
                drawCircle(
                    color = Color.Red,
                    radius = 10f,
                    center = Offset(s.x, s.y*(1+yMinMax))
                )
            }


            // Отрисовка графика
            var x_keys = pointsD.keys.toList()
            var y_values = pointsD.values.toList()
            var decart_points = mutableListOf<Decarts>()

            // Преобразуем экранные координаты нажатых точек в декартовые, это будут узловые точки
            for (i in 0..x_keys.size - 1) {
                var b = Decarts( x_keys[i], y_values[i])
                decart_points.add(b)
            }

            // Построим полином лагранжа по нажатым точкам
            var polinom = Lagrange(decart_points)

            // В списке будут находится точки для отрисовки
            var use_decarts_points = mutableListOf<Decarts>()

            // Сначала разбираемся с иксами
            for(i in 0..scale.w-1){
                var s = ScreenPoints(i.toFloat(), 0f)
                var d = Decarts()
                d.toDecarts(s, scale)
                use_decarts_points.add(d)
            }
            // а теперь вычислим в каждой точке значение полинома
            for(point in use_decarts_points){
                point.y = polinom.count(point.x)
            }

            // Рисуем по получившимя точкам
            for (i in 1..use_decarts_points.size-1) {
                var s1 = ScreenPoints() // Старт
                s1.toScreenPoint(use_decarts_points[i-1], scale)
                var s2 = ScreenPoints() // Конец
                s2.toScreenPoint(use_decarts_points[i], scale)

                drawLine(color = Color.Red, start = Offset(s1.x, s1.y*(1+yMinMax)), end = Offset(s2.x, s2.y*(1+yMinMax)))
            }

        } )

    Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
        Row(modifier = Modifier.padding(10.dp, 10.dp)) {
            Column(modifier = Modifier.padding(10.dp, 10.dp)) {
                Text("xMin")
                TextField(
                    value = xMin.toString(),
                    onValueChange = { text -> xMin = text.toFloatOrNull() ?: -10f })
            }
            Column(modifier = Modifier.padding(10.dp, 10.dp)) {
                Text("xMax")
                TextField(
                    value = xMax.toString(),
                    onValueChange = { text -> xMax = text.toFloatOrNull() ?: 0f})
            }
            Column {
                Row{
                    RadioButton(
                        selected = selectY,
                        onClick = {selectY = true},
                        modifier = Modifier.padding(8.dp)
                    )
                    Text("yMin", fontSize = 22.sp)
                }
                Row{
                    RadioButton(
                        selected = !selectY,
                        onClick = {selectY = false}
                    )
                    Text("ySlider", fontSize = 22.sp)
                }
            }
            Column(modifier = Modifier.padding(10.dp, 10.dp)){
                if(!selectY){
                    Slider(
                        value = yMinMax,
                        valueRange = -1f..1f,
                        steps = 9,
                        onValueChange = {yMinMax = it},
                        )
                }
                else{
                    TextField(
                        value = xMin.toString(),
                        onValueChange = {value -> yMin = value.toFloatOrNull() ?: 0f}
                    )
                }
            }
        }
    }
}

class Scales(var w: Int = 0, var h: Int = 0,
             var xMin: Float = -10f, var xMax: Float = 10f,
             var yMin: Float = -10f, var yMax: Float = -10f){
}

class ScreenPoints(){
    var x: Float = 0f
    var y: Float = 0f

    constructor(X: Float, Y: Float): this(){
        x = X
        y = Y
    }

    // Переводим из декартовых в экранные координаты
    fun toScreenPoint(point: Decarts, scale: Scales): ScreenPoints{
        var indent_y = scale.h / (scale.yMax - scale.yMin)
        var indent_x = scale.w / (scale.yMax - scale.yMin)
        this.y = (scale.yMax-point.y) * indent_y
        this.x = (point.x-scale.yMin) * indent_x
        return ScreenPoints()

    }
}

class Decarts(){
    var x: Float = 0f
    var y: Float = 0f

    constructor(X: Float, Y: Float): this(){
        x = X
        y = Y
    }

    // Переводим из экранных координат в декартовые
    fun toDecarts(point: ScreenPoints ,scale: Scales): Decarts{
        var indent_y = scale.h / (scale.yMax - scale.yMin)
        var indent_x = scale.w / (scale.yMax - scale.yMin)
        this.y = scale.yMax - point.y / indent_y
        this.x = scale.yMin + point.x / indent_x
        return Decarts()
    }
}

class Lagrange(var pnts: MutableList<Decarts>) {

    // Значение полинома Лагранжа в точке
    fun count(x: Float): Float {
        var l_k : Float
        var y_new = 0f;

        for (i in 0 .. pnts.size - 1)
        {
            l_k = pnts[i].y

            for (j in 0 .. pnts.size - 1)
            {
                if (i != j)
                {
                    l_k *= (x - pnts[j].x) / (pnts[i].x - pnts[j].x)
                }
            }
            y_new += l_k
        }
        return y_new
    }

}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
