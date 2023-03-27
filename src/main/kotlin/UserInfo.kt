@kotlinx.serialization.Serializable
data class UserInfo(
    val id: Int,
    val papers: MutableMap<String, Int>,
    var balance: Int
)