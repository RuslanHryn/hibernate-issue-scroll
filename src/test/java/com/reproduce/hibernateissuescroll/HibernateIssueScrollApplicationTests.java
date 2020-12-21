package com.reproduce.hibernateissuescroll;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.hibernate.CacheMode;
import org.hibernate.LazyInitializationException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringBootTest
public class HibernateIssueScrollApplicationTests {

    @Autowired
    private EntityManager entityManager;

    @Test
    public void throwsLazyInitializationException() {
        Post postToPersist = createPostWithTwoComments();
        entityManager.persist(postToPersist);

        /// Flush and remove persisted data from cache
        entityManager.flush();
        entityManager.clear();

        /// Fetch posts via scrollable api
        String query = "select post\n"
                + "from Post post\n";

        ScrollableResults postIterator = execute(query);
        List<Post> toDetach = new ArrayList<>();
        while (postIterator.next()) {
            Post post = (Post) postIterator.get(0);
            post.getTitle();
            toDetach.add(post);

        }
        toDetach.forEach(e -> entityManager.detach(e));
        postIterator.close();

        // Find or select any entity
        assertThrows(PersistenceException.class, () -> entityManager.find(Post.class, 1L));

        // Throws
        // org.hibernate.LazyInitializationException: failed to lazily initialize a collection of role:
        // com.reproduce.hibernateissuescroll.Post.comments, could not initialize proxy - no Session

        // Next execution is successful
        entityManager.find(PostComment.class, 1L);
    }

    @Test
    public void fixUseEagerCollection() {
        Post postToPersist = createPostWithTwoComments();
        entityManager.persist(postToPersist);

        // Flush and remove persisted data from cache
        entityManager.flush();
        entityManager.clear();

        // Fetch posts via scrollable api
        String query = "select post\n"
                + "from Post post\n";

        ScrollableResults postIterator = execute(query);
        List<Post> toDetach = new ArrayList<>();
        while (postIterator.next()) {
            Post post = (Post) postIterator.get(0);
            // Hibernate will perform query to get comments
            // It will prevent LazyInitializationException. Because It will fetch entities in eager collection
            List<PostComment> comments = post.getComments();
            comments.forEach(c -> System.out.println("comment with id " + c.getId()));
            //
            toDetach.add(post);

        }
        toDetach.forEach(e -> entityManager.detach(e));
        postIterator.close();

        assertThat(entityManager.find(PostComment.class, 1L)).isNotNull();
    }

    @Test
    public void fixUseFetchJoinInQueryToGetEagerCollection() {
        Post postToPersist = createPostWithTwoComments();
        entityManager.persist(postToPersist);

        // Flush and remove persisted data from cache
        entityManager.flush();
        entityManager.clear();

        // Fetch posts via scrollable api
        String query = "select post\n"
                + "from Post post\n"
                // Use fetch join to prevent exception
                + "left join fetch post.comments\n";

        ScrollableResults postIterator = execute(query);
        List<Post> toDetach = new ArrayList<>();
        while (postIterator.next()) {
            Post post = (Post) postIterator.get(0);

            toDetach.add(post);
        }
        toDetach.forEach(e -> entityManager.detach(e));
        postIterator.close();

        assertThat(entityManager.find(PostComment.class, 1L)).isNotNull();
    }

    @Test
    public void fixClearCacheAfterIteration() {
        Post postToPersist = createPostWithTwoComments();
        entityManager.persist(postToPersist);

        // Flush and remove persisted data from cache
        entityManager.flush();
        entityManager.clear();

        // Fetch posts via scrollable api
        String query = "select post\n"
                + "from Post post\n";

        ScrollableResults postIterator = execute(query);
        while (postIterator.next()) {
            Post post = (Post) postIterator.get(0);
            post.getTitle();
        }
        entityManager.flush();
        // Clear cache to prevent exception
        entityManager.clear();
        postIterator.close();

        assertThat(entityManager.find(PostComment.class, 1L)).isNotNull();
    }

    @Test
    void fixMakeCollectionLazy() {
        PostLazy postToPersist = createPostWithLazyComment();
        entityManager.persist(postToPersist);

        /// Flush and remove persisted data from cache
        entityManager.flush();
        entityManager.clear();

        /// Fetch posts via scrollable api
        String query = "select post\n"
                + "from PostLazy post\n";

        ScrollableResults postIterator = execute(query);
        List<PostLazy> toDetach = new ArrayList<>();
        while (postIterator.next()) {
            PostLazy post = (PostLazy) postIterator.get(0);
            post.getTitle();
            toDetach.add(post);

        }
        toDetach.forEach(e -> entityManager.detach(e));
        postIterator.close();

        // Find or select any entity
        assertThat(entityManager.find(PostComment.class, 3L)).isNotNull();
    }

    @Test
    public void fixNotUseScroll() {
        Post postToPersist = createPostWithTwoComments();
        entityManager.persist(postToPersist);

        // Flush and remove persisted data from cache
        entityManager.flush();
        entityManager.clear();

        // Fetch posts via scrollable api
        String query = "select post\n"
                + "from Post post\n";

        Iterator postIterator = entityManager.createQuery(query).getResultList().iterator();
        while (postIterator.hasNext()) {
            Post post = (Post) postIterator.next();
            post.getTitle();
            post.getComments().forEach(c -> System.out.println(c.getId()));
        }

        assertThat(entityManager.find(PostComment.class, 1L)).isNotNull();
    }

    private PostLazy createPostWithLazyComment() {
        PostLazy post = new PostLazy();
        post.setId(2L);
        post.setTitle("title");

        List<PostComment> postComments2 = new ArrayList<>();
        PostComment postComment = new PostComment();
        postComment.setPostLazy(post);
        postComment.setId(3L);
        postComment.setReview("review1");
        postComments2.add(postComment);

        post.setComments(postComments2);

        return post;
    }

    private Post createPostWithTwoComments() {
        Post post = new Post();
        post.setTitle("title");
        post.setId(1L);

        List<PostComment> postComments = new ArrayList<>();
        PostComment postComment1 = new PostComment();
        postComment1.setPost(post);
        postComment1.setId(1L);
        postComment1.setReview("review1");
        postComments.add(postComment1);

        PostComment postComment2 = new PostComment();
        postComment2.setPost(post);
        postComment2.setId(2L);
        postComment2.setReview("r2");
        postComments.add(postComment2);

        post.setComments(postComments);

        return post;
    }

    private ScrollableResults execute(String queryString) {
        Query query = entityManager.createQuery(queryString);

        org.hibernate.query.Query unwrapQuery = query.unwrap(org.hibernate.query.Query.class);
        unwrapQuery.setCacheMode(CacheMode.IGNORE);

        return unwrapQuery.scroll(ScrollMode.FORWARD_ONLY);
    }

}
