ProxyGen
========

[![](https://jitpack.io/v/FP-GmbH/ProxyGen.svg)](https://jitpack.io/#FP-GmbH/ProxyGen)

ProxyGen is a tool that uses Kotlin Symbol Processing to generate delegated proxy implementations of Kotlin and Java interfaces or classes which then can be used to replace dependency injection for composable previews.

The generated classes contain delegation properties for each property and each function of the annotated classes. Those delegates are defined as constructor parameters and can be used to inject logic.
The delegate methods will be invoked if the original method is called. The delegated properties take the place of the original ones.

Getting started
---------------
Add Jitpack maven repository to your settings.gradle with

```
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenCentral()
		maven { url = uri("https://jitpack.io") }
	}
}
```
Add dependencies to build.gradle of your module

```
dependencies {
    implementation("com.github.FP-GmbH.ProxyGen:core:{latestVersion}")
    ksp("com.github.FP-GmbH.ProxyGen:compiler:{latestVersion}")
}
```

How to use?
-----------
Annotate desired classes with `@ProxyGen` annotation.

```
@ProxyGen
interface UserRepository {

    var isLoggedIn: Boolean

    suspend fun getLoggedInUser(): User?
}
```

Build your module to generate the proxy classes.

```
public class UserRepositoryProxy(
  private var isLoggedInDelegate: Boolean? = null,
  private val getLoggedInUserDelegate: (suspend () -> User?)? = null,
) : UserRepository {

  override var isLoggedIn: Boolean
    get() = isLoggedInDelegate ?: TODO("Not yet implemented")
    set(`value`) {
      isLoggedInDelegate = value
    }

  override suspend fun getLoggedInUser(): User? = getLoggedInUserDelegate?.invoke() ?: TODO("Not yet implemented")
}
```

Use generated classes.

```
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val userName = viewModel.loggedInUser.value?.name ?: "not logged in"

    Text(
        text = "Hello $userName",
        modifier = modifier,
    )
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    val loggedInUser = mutableStateOf<User?>(null)

    init {
        viewModelScope.launch {
            loggedInUser.value = userRepository.getLoggedInUser()
        }
    }
}

@Preview
@Composable
fun LoginScreenPreview() {
    val userRepository = UserRepositoryProxy(
        isLoggedInDelegate = false,
        getLoggedInUserDelegate = { null },
    )

    Scaffold {
        LoginScreen(
            viewModel = LoginViewModel(
                userRepository = userRepository
            ),
            modifier = Modifier.padding(it),
        )
    }
}
```

Known issues
------------
| Problem            | Description                                                                                                              | Solution                                                 |
|--------------------|--------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| Method Overloading | An annotated class cannot have several methods with the same name. The generated delegates would have the same name, causing a conflict. | If you have to overload a method use default parameters. |

Sample
------

If you wand to run the sample app from the source code, reactivate the sample module in the settings.gradle file. I had to comment it out for deployment.

License
=======
Apache 2.0. See the [LICENSE][1] file for details.

[1]: https://github.com/FP-GmbH/ProxyGen/blob/main/LICENSE
