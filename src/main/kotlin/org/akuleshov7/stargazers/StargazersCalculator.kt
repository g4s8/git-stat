package org.akuleshov7.stargazers

import org.akuleshov7.api.StargazersJson
import org.akuleshov7.api.stargazersEndPoint
import org.akuleshov7.utils.HttpClientFactory
import org.akuleshov7.utils.logAndExit
import org.akuleshov7.utils.logInfo

class StargazersCalculator(private val repos: String?, private val extended: Boolean, val configPath: String?) {
    lateinit var uniqueStargazers: List<String>
    lateinit var duplicatedStargazers: Map<StargazersJson, Int>
    var numberOfDuplicatedStars: Int = 0

    suspend fun calculateStargazers(): StargazersCalculator{
        // creating correct urls from the list of repos
        val repositoriesList: Set<String>? = (readConfig() ?: repos)
            ?.split(',')
            ?.map { it.trim() }
            ?.map { it.stargazersEndPoint() }
            ?.toSet()

        // preparing correct urls to github from the list of repos
        if (repositoriesList != null) {
            val allStargazers = HttpClientFactory(repositoriesList)
                .requestAllData<Array<StargazersJson>>()
                // flatten list<array> with stargazers
                .flatMap { array ->
                    mutableListOf<StargazersJson>()
                        .also {
                            it.addAll(array)
                        }
                }

            // calculate number of stars and stargazers
            doCalculations(allStargazers)

        } else {
            "List of repositories was not provided. It can be provided in the configuration file or with a '-r' option" logAndExit 8
        }

        return this
    }

    fun log() {
        logInfo("Number of stargazers: ${uniqueStargazers.size}")
        logInfo("Number of duplicated stars: $numberOfDuplicatedStars")

        if (extended) {
            logInfo("Unique stargazers: ${uniqueStargazers.joinToString()}")
            logInfo("Duplicated stars: ${
                duplicatedStargazers
                    .map { "User:${it.key.login} Stars:${it.value}" }
                    .joinToString("; ")
            }"
            )
        }
    }

    private fun doCalculations(allStargazers: List<StargazersJson>) {
        val groupedStargazers = allStargazers.groupingBy { it }.eachCount()
        uniqueStargazers = groupedStargazers.keys.map { it.login }
        duplicatedStargazers = groupedStargazers.filter { it.value > 1 }
        numberOfDuplicatedStars = duplicatedStargazers.values.map { it - 1 }.sum()
    }


    private fun readConfig(): String? {
        // FixMe: implement and move out of this class
        return null
    }
}
