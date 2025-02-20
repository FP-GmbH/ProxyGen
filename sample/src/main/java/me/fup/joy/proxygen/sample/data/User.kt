package me.fup.joy.proxygen.sample.data

data class User(
    val id: String,
    val name: String
) {

    companion object {

        fun createDummy(id: String = "0", name: String = "dummy") = User(
            id = id,
            name = name
        )
    }
}