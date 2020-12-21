package com.reproduce.hibernateissuescroll;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class PostComment {

    @Id
    private Long id;

    @ManyToOne
    private Post post;

    @ManyToOne
    private PostLazy postLazy;

    private String review;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public PostLazy getPostLazy() {
        return postLazy;
    }

    public void setPostLazy(PostLazy postLazy) {
        this.postLazy = postLazy;
    }
}
