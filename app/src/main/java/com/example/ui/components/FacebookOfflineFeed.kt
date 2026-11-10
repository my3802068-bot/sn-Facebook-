package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FacebookPost
import com.example.model.FacebookSampleData
import com.example.model.FacebookStory
import com.example.ui.theme.FbBackgroundLight
import com.example.ui.theme.FbBlue
import com.example.ui.theme.FbTextSecondaryLight

@Composable
fun FacebookOfflineFeed(
    onRetryConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var posts by remember { mutableStateOf(FacebookSampleData.posts) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FbBackgroundLight)
            .testTag("facebook_offline_feed")
    ) {
        // Facebook Top Header Bar
        FacebookHeader()

        // Facebook Main Navigation Tabs
        FacebookTabs(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

        // Offline Notification Banner with Retry button
        OfflineNoticeBanner(onRetry = onRetryConnection)

        // Main Feed Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "What's on your mind?" Post Creator Box
            item {
                CreatePostCard()
            }

            // Stories Carousel
            item {
                StoriesSection(stories = FacebookSampleData.stories)
            }

            // News Feed Posts
            items(posts, key = { it.id }) { post ->
                FacebookPostCard(
                    post = post,
                    onToggleLike = {
                        posts = posts.map { current ->
                            if (current.id == post.id) {
                                val nextLiked = !current.isLiked
                                current.copy(
                                    isLiked = nextLiked,
                                    initialLikes = if (nextLiked) current.initialLikes + 1 else current.initialLikes - 1
                                )
                            } else current
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun FacebookHeader() {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Facebook Logo Title
            Text(
                text = "facebook",
                color = FbBlue,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )

            // Right header buttons (Search, Messenger)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE4E6EB))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Facebook",
                        tint = Color(0xFF050505),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE4E6EB))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Messenger",
                        tint = Color(0xFF050505),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FacebookTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val icons = listOf(
        Icons.Default.Home to "Home",
        Icons.Default.Person to "Friends",
        Icons.Default.OndemandVideo to "Watch",
        Icons.Default.Storefront to "Marketplace",
        Icons.Default.Notifications to "Notifications",
        Icons.Default.Menu to "Menu"
    )

    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.White,
        contentColor = FbBlue,
        divider = { HorizontalDivider(color = Color(0xFFCED0D4), thickness = 0.5.dp) }
    ) {
        icons.forEachIndexed { index, pair ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = pair.first,
                        contentDescription = pair.second,
                        tint = if (selectedTab == index) FbBlue else Color(0xFF65676B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun OfflineNoticeBanner(onRetry: () -> Unit) {
    Surface(
        color = Color(0xFFFFF3CD),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📡 Offline Preview Mode",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF856404)
                )
                Text(
                    text = "Tap SN tool > Reload to connect to live Facebook",
                    fontSize = 11.sp,
                    color = Color(0xFF856404)
                )
            }

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CreatePostCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // User Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(FbBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Post Input Field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF0F2F5))
                        .clickable { }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "What's on your mind?",
                        color = FbTextSecondaryLight,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFE4E6EB), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Post action buttons (Live, Photo, Room)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { }
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoCall,
                        contentDescription = "Live",
                        tint = Color(0xFFF02849),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Live", fontSize = 12.sp, color = FbTextSecondaryLight)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { }
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Photo",
                        tint = Color(0xFF45BD62),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Photo", fontSize = 12.sp, color = FbTextSecondaryLight)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Room",
                        tint = Color(0xFF8E24AA),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Room", fontSize = 12.sp, color = FbTextSecondaryLight)
                }
            }
        }
    }
}

@Composable
private fun StoriesSection(stories: List<FacebookStory>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        LazyRow(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(stories, key = { it.id }) { story ->
                StoryCard(story = story)
            }
        }
    }
}

@Composable
private fun StoryCard(story: FacebookStory) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { },
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(story.colorHex),
                            Color(story.colorHex).copy(alpha = 0.8f),
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
                .padding(8.dp)
        ) {
            if (story.isUserStory) {
                // Add story icon button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .align(Alignment.TopStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add story",
                        tint = FbBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                // Profile circle border
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(2.dp, FbBlue, CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                        .align(Alignment.TopStart),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = story.authorName.take(1),
                        fontWeight = FontWeight.Bold,
                        color = Color(story.colorHex),
                        fontSize = 14.sp
                    )
                }
            }

            Text(
                text = story.authorName,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun FacebookPostCard(
    post: FacebookPost,
    onToggleLike: () -> Unit
) {
    val likeColor by animateColorAsState(
        targetValue = if (post.isLiked) FbBlue else Color(0xFF65676B),
        label = "LikeColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
            // Post Author Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E88E5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.authorName.take(1),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF050505)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.timeAgo,
                            fontSize = 11.sp,
                            color = FbTextSecondaryLight
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Public",
                            tint = FbTextSecondaryLight,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            // Post Text
            Text(
                text = post.content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFF050505),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )

            // Reactions Count Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(FbBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.initialLikes}",
                        fontSize = 12.sp,
                        color = FbTextSecondaryLight
                    )
                }

                Text(
                    text = "${post.initialComments} comments • ${post.initialShares} shares",
                    fontSize = 12.sp,
                    color = FbTextSecondaryLight
                )
            }

            HorizontalDivider(color = Color(0xFFE4E6EB), thickness = 0.5.dp)

            // Like, Comment, Share Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Like Button
                Row(
                    modifier = Modifier
                        .clickable { onToggleLike() }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Like",
                        tint = likeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Like",
                        fontSize = 13.sp,
                        fontWeight = if (post.isLiked) FontWeight.Bold else FontWeight.Normal,
                        color = likeColor
                    )
                }

                // Comment Button
                Row(
                    modifier = Modifier
                        .clickable { }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Comment,
                        contentDescription = "Comment",
                        tint = Color(0xFF65676B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Comment", fontSize = 13.sp, color = Color(0xFF65676B))
                }

                // Share Button
                Row(
                    modifier = Modifier
                        .clickable { }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF65676B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontSize = 13.sp, color = Color(0xFF65676B))
                }
            }
        }
    }
}
