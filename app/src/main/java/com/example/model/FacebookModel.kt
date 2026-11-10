package com.example.model

data class FacebookStory(
    val id: String,
    val authorName: String,
    val authorAvatarUrl: String? = null,
    val storyImageUrl: String? = null,
    val isUserStory: Boolean = false,
    val colorHex: Long = 0xFF1877F2
)

data class FacebookPost(
    val id: String,
    val authorName: String,
    val timeAgo: String,
    val content: String,
    val imageUrl: String? = null,
    val initialLikes: Int,
    val initialComments: Int,
    val initialShares: Int,
    var isLiked: Boolean = false
)

object FacebookSampleData {
    val stories = listOf(
        FacebookStory(
            id = "s0",
            authorName = "Create Story",
            isUserStory = true,
            colorHex = 0xFF1877F2
        ),
        FacebookStory(
            id = "s1",
            authorName = "Tanvir Ahmed",
            colorHex = 0xFF8E24AA
        ),
        FacebookStory(
            id = "s2",
            authorName = "Rahim Islam",
            colorHex = 0xFF00897B
        ),
        FacebookStory(
            id = "s3",
            authorName = "Shakil Khan",
            colorHex = 0xFFE65100
        ),
        FacebookStory(
            id = "s4",
            authorName = "Tech Bangladesh",
            colorHex = 0xFF1E88E5
        )
    )

    val posts = listOf(
        FacebookPost(
            id = "p1",
            authorName = "SN Community & Tech",
            timeAgo = "10m • 🌐",
            content = "Welcome to SN Facebook! You can use the floating 'SN Mini Tool' to quickly reload pages, clear cache/cookies data, or switch between desktop and mobile mode! 🚀🇧🇩",
            initialLikes = 142,
            initialComments = 28,
            initialShares = 14
        ),
        FacebookPost(
            id = "p2",
            authorName = "Shakil Hasan",
            timeAgo = "1h • 👥",
            content = "Alhamdulillah! Finally created our custom Facebook companion app with draggable floating Windows Mini controller. Try dragging the SN badge anywhere on the screen!",
            initialLikes = 89,
            initialComments = 16,
            initialShares = 5
        ),
        FacebookPost(
            id = "p3",
            authorName = "Bangla Tech Updates",
            timeAgo = "3h • 🌐",
            content = "Facebook tip: If you face slow loading or session glitches, tap the floating SN Windows tool and press 'Clear Data' or 'Reload' to refresh immediately.",
            initialLikes = 312,
            initialComments = 54,
            initialShares = 43
        )
    )
}
