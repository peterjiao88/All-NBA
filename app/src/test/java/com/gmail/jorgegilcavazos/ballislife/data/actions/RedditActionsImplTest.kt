package com.gmail.jorgegilcavazos.ballislife.data.actions

import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository
import com.gmail.jorgegilcavazos.ballislife.data.reddit.RedditAuthentication
import com.gmail.jorgegilcavazos.ballislife.data.repository.comments.ContributionRepository
import com.gmail.jorgegilcavazos.ballislife.data.service.RedditService
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.TrampolineSchedulerProvider
import io.reactivex.Completable
import io.reactivex.Single
import net.dean.jraw.RedditClient
import net.dean.jraw.http.UserAgent
import net.dean.jraw.models.Comment
import net.dean.jraw.models.Submission
import net.dean.jraw.models.VoteDirection
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

class RedditActionsImplTest {

  companion object {
    val USERNAME = "USERNAME"
  }

  @Mock private lateinit var mockRedditAuthentication: RedditAuthentication
  @Mock private lateinit var mockRedditService: RedditService
  @Mock private lateinit var mockContributionsRepository: ContributionRepository
  @Mock private lateinit var mockLocalRepository: LocalRepository

  private lateinit var redditActions: RedditActions
  private val redditClient = RedditClient(UserAgent.of("user agent"))

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)

    `when`(mockRedditAuthentication.authenticate()).thenReturn(Completable.complete())
    `when`(mockRedditAuthentication.redditClient).thenReturn(redditClient)
    `when`(mockLocalRepository.username).thenReturn(USERNAME)

    redditActions = RedditActionsImpl(
        mockRedditAuthentication,
        mockRedditService,
        mockContributionsRepository,
        TrampolineSchedulerProvider(),
        mockLocalRepository)
  }

  @Test
  fun saveContributionWhenUserLoggedIn() {
    val mockComment = mock(Comment::class.java)
    `when`(mockRedditService.savePublicContribution(redditClient, mockComment))
        .thenReturn(Completable.complete())
    `when`(mockRedditAuthentication.checkUserLoggedIn()).thenReturn(Single.just(true))

    val testObserver = redditActions.savePublicContribution(mockComment).test()

    testObserver.assertValueCount(2)
    testObserver.assertValueAt(0, { it.inProgress })
    testObserver.assertValueAt(1, { it.success })
  }

  @Test
  fun saveContributionWhenUserNotLoggedIn() {
    val mockComment = mock(Comment::class.java)
    `when`(mockRedditService.savePublicContribution(redditClient, mockComment))
        .thenReturn(Completable.complete())
    `when`(mockRedditAuthentication.checkUserLoggedIn()).thenReturn(Single.just(false))

    val testObserver = redditActions.savePublicContribution(mockComment).test()

    testObserver.assertValueCount(2)
    testObserver.assertValueAt(0, { it.inProgress })
    testObserver.assertValueAt(1, { it.notLoggedIn })
  }

  @Test
  fun saveContributionWrapError() {
    val mockComment = mock(Comment::class.java)
    val error = Exception()
    `when`(mockRedditService.savePublicContribution(redditClient, mockComment))
        .thenReturn(Completable.error(error))
    `when`(mockRedditAuthentication.checkUserLoggedIn()).thenReturn(Single.just(true))

    val testObserver = redditActions.savePublicContribution(mockComment).test()

    testObserver.assertValueCount(2)
    testObserver.assertValueAt(0, { it.inProgress })
    testObserver.assertValueAt(1, { it.error == error })
  }

  @Test
  fun unsaveContributionWhenUserLoggedIn() {
    val mockComment = mock(Comment::class.java)
    `when`(mockRedditService.unsavePublicContribution(redditClient, mockComment))
        .thenReturn(Completable.complete())
    `when`(mockRedditAuthentication.checkUserLoggedIn()).thenReturn(Single.just(true))

    val testObserver = redditActions.unsavePublicContribution(mockComment).test()

    testObserver.assertValueCount(2)
    testObserver.assertValueAt(0, { it.inProgress })
    testObserver.assertValueAt(1, { it.success })
  }

  @Test
  fun unsaveContributionWhenUserNotLoggedIn() {
    val mockComment = mock(Comment::class.java)
    `when`(mockRedditService.unsavePublicContribution(redditClient, mockComment))
        .thenReturn(Completable.complete())
    `when`(mockRedditAuthentication.checkUserLoggedIn()).thenReturn(Single.just(false))

    val testObserver = redditActions.unsavePublicContribution(mockComment).test()

    testObserver.assertValueCount(2)
    testObserver.assertValueAt(0, { it.inProgress })
    testObserver.assertValueAt(1, { it.notLoggedIn })
  }

  @Test
  fun unsaveContributionWrapError() {
    val mockComment = mock(Comment::class.java)
    val error = Exception()
    `when`(mockRedditService.unsavePublicContribution(redditClient, mockComment))
        .thenReturn(Completable.error(error))
    `when`(mockRedditAuthentication.checkUserLoggedIn()).thenReturn(Single.just(true))

    val testObserver = redditActions.unsavePublicContribution(mockComment).test()

    testObserver.assertValueCount(2)
    testObserver.assertValueAt(0, { it.inProgress })
    testObserver.assertValueAt(1, { it.error == error })
  }

  @Test
  fun voteCommentWhenUserLoggedIn() {
    val mockComment = mock(Comment::class.java)
    `when`(mockRedditService.voteComment(redditClient, mockComment, VoteDirection.UPVOTE))
        .thenReturn(Completable.complete())
    `when`(mockRedditAuthentication.checkUserLoggedIn()).thenReturn(Single.just(true))

    val testObserver = redditActions.voteComment(mockComment, VoteDirection.UPVOTE).test()

    testObserver.assertValueCount(2)
    testObserver.assertValueAt(0, { it.inProgress })
    testObserver.assertValueAt(1, { it.success })
  }

  @Test
  fun voteCommentWhenUserNotLoggedIn() {
    val mockComment = mock(Comment::class.java)
    `when`(mockRedditService.voteComment(redditClient, mockComment, VoteDirection.UPVOTE))
        .thenReturn(Completable.complete())
    `when`(mockRedditAuthentication.checkUserLoggedIn()).thenReturn(Single.just(false))

    val testObserver = redditActions.voteComment(mockComment, VoteDirection.UPVOTE).test()

    testObserver.assertValueCount(2)
    testObserver.assertValueAt(0, { it.inProgress })
    testObserver.assertValueAt(1, { it.notLoggedIn })
  }

  @Test
  fun voteCommentWrapError() {
    val error = Exception()
    val mockComment = mock(Comment::class.java)
    `when`(mockRedditService.voteComment(redditClient, mockComment, VoteDirection.UPVOTE))
        .thenReturn(Completable.error(error))
    `when`(mockRedditAuthentication.checkUserLoggedIn()).thenReturn(Single.just(true))

    val testObserver = redditActions.voteComment(mockComment, VoteDirection.UPVOTE).test()

    testObserver.assertValueCount(2)
    testObserver.assertValueAt(0, { it.inProgress })
    testObserver.assertValueAt(1, { it.error == error })
  }

  @Test
  fun replyToCommentWhenUserLoggedIn() {
    val id = "abcdefgh"
    val response = "A reply!"
    val mockComment = mock(Comment::class.java)
    `when`(mockRedditAuthentication.checkUserLoggedIn()).thenReturn(Single.just(true))
    `when`(mockContributionsRepository.getComment(id)).thenReturn(mockComment)
    `when`(mockRedditService.replyToComment(redditClient, mockComment, response))
        .thenReturn(Single.just(""))

    val testObserver = redditActions.replyToComment(id, response).test()

    testObserver.assertValueCount(2)
    testObserver.assertValueAt(0, { it.inProgress })
    testObserver.assertValueAt(1, { it.success })
  }

  @Test
  fun replyToCommentWhenUserLoggedInButParentNotFound() {
    val fullname = "abcdefgh"
    `when`(mockRedditAuthentication.checkUserLoggedIn()).thenReturn(Single.just(true))
    `when`(mockContributionsRepository.getComment(fullname)).thenReturn(null)

    val testObserver = redditActions.replyToComment(fullname, "A reply!").test()

    testObserver.assertValueCount(2)
    testObserver.assertValueAt(0, { it.inProgress })
    testObserver.assertValueAt(1, { it.parentNotFound })
  }

  @Test
  fun replyToCommentWhenUserNotLoggedIn() {
    `when`(mockRedditAuthentication.checkUserLoggedIn()).thenReturn(Single.just(false))

    val testObserver = redditActions.replyToComment("abc", "A reply!").test()

    testObserver.assertValueCount(2)
    testObserver.assertValueAt(0, { it.inProgress })
    testObserver.assertValueAt(1, { it.notLoggedIn })
  }

  @Test
  fun replyToSubmissionWhenUserLoggedIn() {
    val id = "abcdefgh"
    val response = "A reply!"
    val mockSubmission = mock(Submission::class.java)
    `when`(mockRedditAuthentication.checkUserLoggedIn()).thenReturn(Single.just(true))
    `when`(mockContributionsRepository.getSubmission(id)).thenReturn(mockSubmission)
    `when`(mockRedditService.replyToThread(redditClient, mockSubmission, response))
        .thenReturn(Single.just(""))

    val testObserver = redditActions.replyToSubmission(id, response).test()

    testObserver.assertValueCount(2)
    testObserver.assertValueAt(0, { it.inProgress })
    testObserver.assertValueAt(1, { it.success })
  }

  @Test
  fun replyToSubmissionWhenUserLoggedInButParentNotFound() {
    val id = "abcdefgh"
    `when`(mockRedditAuthentication.checkUserLoggedIn()).thenReturn(Single.just(true))
    `when`(mockContributionsRepository.getSubmission(id)).thenReturn(null)

    val testObserver = redditActions.replyToSubmission(id, "A reply!").test()

    testObserver.assertValueCount(2)
    testObserver.assertValueAt(0, { it.inProgress })
    testObserver.assertValueAt(1, { it.parentNotFound })
  }

  @Test
  fun replyToSubmissionWhenUserNotLoggedIn() {
    `when`(mockRedditAuthentication.checkUserLoggedIn()).thenReturn(Single.just(false))

    val testObserver = redditActions.replyToSubmission("abc", "A reply!").test()

    testObserver.assertValueCount(2)
    testObserver.assertValueAt(0, { it.inProgress })
    testObserver.assertValueAt(1, { it.notLoggedIn })
  }

  @Test
  fun replyToSubmissionWrapError() {
    val id = "abcdefgh"
    val response = "A reply!"
    val error = Exception()
    val mockSubmission = mock(Submission::class.java)
    `when`(mockRedditAuthentication.checkUserLoggedIn()).thenReturn(Single.just(true))
    `when`(mockContributionsRepository.getSubmission(id)).thenReturn(mockSubmission)
    `when`(mockRedditService.replyToThread(redditClient, mockSubmission, response))
        .thenReturn(Single.error(error))

    val testObserver = redditActions.replyToSubmission(id, response).test()

    testObserver.assertValueCount(2)
    testObserver.assertValueAt(0, { it.inProgress })
    testObserver.assertValueAt(1, { it.error == error })
  }
}