package com.gmail.jorgegilcavazos.ballislife.features.submission

import com.gmail.jorgegilcavazos.ballislife.base.BasePresenter
import com.gmail.jorgegilcavazos.ballislife.data.actions.RedditActions
import com.gmail.jorgegilcavazos.ballislife.data.actions.models.ReplyUIModel
import com.gmail.jorgegilcavazos.ballislife.data.actions.models.SaveUIModel
import com.gmail.jorgegilcavazos.ballislife.data.reddit.RedditAuthentication
import com.gmail.jorgegilcavazos.ballislife.data.repository.comments.ContributionRepository
import com.gmail.jorgegilcavazos.ballislife.data.repository.submissions.SubmissionRepository
import com.gmail.jorgegilcavazos.ballislife.features.model.ThreadItem
import com.gmail.jorgegilcavazos.ballislife.util.*
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.BaseSchedulerProvider
import com.google.common.base.Optional
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import net.dean.jraw.models.Comment
import net.dean.jraw.models.CommentSort
import net.dean.jraw.models.Submission
import net.dean.jraw.models.VoteDirection
import javax.inject.Inject

class SubmissionPresenter @Inject constructor(
    private val redditAuthentication: RedditAuthentication,
    private val submissionRepository: SubmissionRepository,
    private val schedulerProvider: BaseSchedulerProvider,
    private val disposables: CompositeDisposable,
    private val redditActions: RedditActions,
    private val contributionRepository: ContributionRepository,
    private val networkUtils: NetworkUtils,
    private val errorHandler: ErrorHandler) : BasePresenter<SubmissionView>() {

  private var currentSubmission: Submission? = null

  override fun attachView(view: SubmissionView) {
    super.attachView(view)

    view.commentSaves()
        .subscribe { saveComment(it) }
        .addTo(disposables)

    view.commentUnsaves()
        .subscribe { unsaveComment(it) }
        .addTo(disposables)

    view.commentUpvotes()
        .subscribe {
          redditActions.voteComment(it, VoteDirection.UPVOTE)
              .subscribe {
                if (it.notLoggedIn) {
                  view.showNotLoggedInError()
                }
              }.addTo(disposables)
        }.addTo(disposables)

    view.commentDownvotes()
        .subscribe {
          redditActions.voteComment(it, VoteDirection.DOWNVOTE)
              .subscribe {
                if (it.notLoggedIn) {
                  view.showNotLoggedInError()
                }
              }.addTo(disposables)
        }.addTo(disposables)

    view.commentNovotes()
        .subscribe {
          redditActions.voteComment(it, VoteDirection.NO_VOTE)
              .subscribe {
                if (it.notLoggedIn) {
                  view.showNotLoggedInError()
                }
              }.addTo(disposables)
        }.addTo(disposables)

    view.submissionSaves()
        .subscribe {
          redditActions.savePublicContribution(it)
              .subscribe {
                if (it.notLoggedIn) {
                  view.showNotLoggedInError()
                }
              }
        }
        .addTo(disposables)

    view.submissionUnsaves()
        .subscribe {
          redditActions.unsavePublicContribution(it)
              .subscribe {
                if (it.notLoggedIn) {
                  view.showNotLoggedInError()
                }
              }.addTo(disposables)
        }.addTo(disposables)

    view.submissionUpvotes()
        .subscribe {
          redditActions.voteSubmission(it, VoteDirection.UPVOTE)
              .subscribe {
                if (it.notLoggedIn) {
                  view.showNotLoggedInError()
                }
              }.addTo(disposables)
        }.addTo(disposables)

    view.submissionDownvotes()
        .subscribe {
          redditActions.voteSubmission(it, VoteDirection.DOWNVOTE)
              .subscribe {
                if (it.notLoggedIn) {
                  view.showNotLoggedInError()
                }
              }.addTo(disposables)
        }.addTo(disposables)

    view.submissionNovotes()
        .subscribe {
          redditActions.voteSubmission(it, VoteDirection.NO_VOTE)
              .subscribe {
                if (it.notLoggedIn) {
                  view.showNotLoggedInError()
                }
              }.addTo(disposables)
        }.addTo(disposables)

    view.commentReplies()
        .subscribe {
          if (redditAuthentication.isUserLoggedIn) {
            contributionRepository.saveComment(it)
            view.openReplyToCommentActivity(it)
          } else {
            view.showNotLoggedInError()
          }
        }.addTo(disposables)

    view.submissionReplies()
        .subscribe {
          if (redditAuthentication.isUserLoggedIn) {
            contributionRepository.saveSubmission(
                currentSubmission
                    ?: throw IllegalStateException("Current submission should not be null"))
            view.openReplyToSubmissionActivity(
                currentSubmission?.id
                    ?: throw IllegalStateException("Current submission should not be null"))
          } else {
            view.showNotLoggedInError()
          }
        }.addTo(disposables)

    view.submissionContentClicks()
        .subscribe { onContentClick(it) }
        .addTo(disposables)
  }

  override fun detachView() {
    disposables.clear()
    super.detachView()
  }

  fun loadComments(threadId: String, sorting: CommentSort, forceReload: Boolean) {
    loadComments(threadId, sorting, null, forceReload)
  }

  fun loadComments(
      threadId: String,
      sorting: CommentSort,
      commentIdToScroll: String?,
      forceReload: Boolean) {
    view.hideFab()
    view.setLoadingIndicator(true)

    redditAuthentication.authenticate()
        .andThen(
            submissionRepository.getSubmission(
                threadId,
                sorting,
                forceReload))
        .subscribeOn(schedulerProvider.io())
        .observeOn(schedulerProvider.ui())
        .subscribe(
            { submissionWrapper ->
              currentSubmission = submissionWrapper.submission!!
              val items = CommentsTraverser
                  .flattenCommentTree(submissionWrapper.submission.comments.children)

              val pos = findComment(items, commentIdToScroll)

              view.showComments(items, submissionWrapper.submission)
              view.setLoadingIndicator(false)
              view.showFab()

              if (pos.isPresent) {
                view.scrollToComment(pos.get())
              }
            },
            { e ->
              view.setLoadingIndicator(false)
            }
        )
        .addTo(disposables)
  }

  fun replyToComment(parentFullname: String, response: String) {
    redditActions.replyToComment(parentFullname, response)
        .subscribe(
            { uiModel: ReplyUIModel ->
              if (uiModel.success) {
                val commentResponse = uiModel.commentItem
                val parentComment = contributionRepository.getComment(parentFullname)
                if (uiModel.commentItem != null && parentComment != null) {
                  view.addCommentItem(uiModel.commentItem, parentComment.fullName)
                }
              }
            },
            { e ->
            }
        )
  }

  fun replyToSubmission(submissionId: String, response: String) {
    redditActions.replyToSubmission(submissionId, response)
        .subscribe(
            { uiModel ->
            },
            { e ->
            }
        )
  }

  private fun saveComment(comment: Comment) {
    redditActions.savePublicContribution(comment)
        .subscribe(
            { uiModel: SaveUIModel ->
              if (uiModel.notLoggedIn) {
                view.showNotLoggedInError()
              }
            }
        )
        .addTo(disposables)
  }

  private fun unsaveComment(comment: Comment) {
    redditActions.unsavePublicContribution(comment)
        .subscribe(
            {
              if (it.notLoggedIn) {
                view.showNotLoggedInError()
              }
            }
        )
        .addTo(disposables)
  }

  private fun onContentClick(url: String?) {
    if (url != null) {
      if (url.contains(Constants.STREAMABLE_DOMAIN)) {
        val shortCode = Utilities.getStreamableShortcodeFromUrl(url)
        if (shortCode != null) {
          view.openStreamable(shortCode)
        } else {
          view.openContentTab(url)
        }
      } else {
        view.openContentTab(url)
      }
    } else {
      view.showContentUnavailableToast()
    }
  }

  private fun findComment(items: List<ThreadItem>, id: String?): Optional<Int> {
    if (id == null) {
      return Optional.absent()
    }
    for (i in items.indices) {
      val node = items[i].commentNode
      if (node != null && node.comment.id == id) {
        return Optional.of(i)
      }
    }
    return Optional.absent()
  }
}
