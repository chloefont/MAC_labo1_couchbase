package ch.heig.mac;

import java.util.List;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.QueryOptions;


public class Requests {
    private final Cluster cluster;

    public Requests(Cluster cluster) {
        this.cluster = cluster;
    }

    public List<String> getCollectionNames() {
        QueryResult result = cluster.query(
                "SELECT RAW r.name\n" +
                        "FROM system:keyspaces r\n" +
                        "WHERE r.`bucket` = \"mflix-sample\";"
        );
        
        return result.rowsAs(String.class);
    }

    public List<JsonObject> inconsistentRating() {
        QueryResult result = cluster.query(
            "SELECT imdb.id as imdb_id, tomatoes.viewer.rating as tomatoes_rating, imdb.rating as imdb_rating\n" +
            "FROM `mflix-sample`._default.movies\n" +
            "WHERE tomatoes.viewer.rating IS NOT MISSING AND tomatoes.viewer.rating > 0 " +
                "AND Abs(tomatoes.viewer.rating - imdb.rating) > 7;"
        );

        return result.rowsAs(JsonObject.class);
    }

    public List<JsonObject> hiddenGem() {
        QueryResult result = cluster.query(
            "SELECT title\n" +
            "FROM `mflix-sample`._default.movies\n" +
            "WHERE tomatoes.critic.rating = 10 AND tomatoes.viewer.rating IS MISSING;"
        );

        return result.rowsAs(JsonObject.class);
    }

    public List<JsonObject> topReviewers() {
        QueryResult result = cluster.query(
            "SELECT comments.name, COUNT(comments.name) as cnt\n" +
            "FROM `mflix-sample`._default.comments\n" +
            "GROUP BY comments.name\n" +
            "ORDER BY cnt DESC\n" +
            "LIMIT 10;"
        );

        return result.rowsAs(JsonObject.class);
    }

    public List<String> greatReviewers() {
        QueryResult result = cluster.query(
            "SELECT RAW comments.name\n" +
            "FROM `mflix-sample`._default.comments\n" +
            "GROUP BY comments.name\n" +
            "HAVING COUNT(comments.name) > 300"
        );

        return result.rowsAs(String.class);
    }

    public List<JsonObject> bestMoviesOfActor(String actor) {
        QueryResult result = cluster.query(
            "SELECT imdb.id AS imdb_id, imdb.rating, `cast`\n" +
            "FROM `mflix-sample`._default.movies\n" +
            "WHERE imdb.rating > 8 AND \"" + actor +"\" IN `cast`;"
        );


        return result.rowsAs(JsonObject.class);
    }

    public List<JsonObject> plentifulDirectors() {
        QueryResult result = cluster.query(
            "SELECT director AS director_name, COUNT(*) AS count_film\n" +
            "FROM `mflix-sample`._default.movies\n" +
                "UNNEST directors director\n" +
            "GROUP BY director\n" +
            "HAVING COUNT(*) > 30;"
        );


        return result.rowsAs(JsonObject.class);
    }

    public List<JsonObject> confusingMovies() {
        QueryResult result = cluster.query(
            "SELECT movies._id as movie_id, movies.title\n" + 
            "FROM `mflix-sample`._default.movies\n" +
            "WHERE ARRAY_LENGTH(movies.directors) > 20"
        );

        return result.rowsAs(JsonObject.class);
    }

    public List<JsonObject> commentsOfDirector1(String director) {
        QueryResult result = cluster.query(
            "SELECT comments.movie_id, comments.text\n" +
            "FROM `mflix-sample`._default.comments\n" +
                "INNER JOIN `mflix-sample`._default.movies\n" + 
                    "ON comments.movie_id = movies._id\n" +
            "WHERE \"" + director + "\" IN movies.directors;"
        );

        return result.rowsAs(JsonObject.class);
    }

    public List<JsonObject> commentsOfDirector2(String director) {
        QueryResult result = cluster.query(
            "SELECT comments.movie_id, comments.text\n" +
            "FROM `mflix-sample`._default.comments\n" +
            "WHERE comments.movie_id IN (\n" +
                "SELECT DISTINCT RAW movies._id\n" +
                "FROM `mflix-sample`._default.movies\n" +
                "WHERE \"" + director + "\" IN movies.directors);"
        );

        return result.rowsAs(JsonObject.class);
    }

    // Returns the number of documents updated.
    public long removeEarlyProjection(String movieId) {
        QueryResult result = cluster.query(
            "UPDATE `mflix-sample`._default.theaters AS theaters\n" + 
            "SET theaters.schedule = ARRAY s FOR s IN theaters.schedule\n" + 
            "WHEN s.hourBegin >= \"18:00\" OR s.movieId != \"" + movieId +"\" END;"
        , QueryOptions.queryOptions().metrics(true));

        return result.metaData().metrics().get().mutationCount();
    }

    public List<JsonObject> nightMovies() {
        QueryResult result = cluster.query(
            "SELECT movies.title, movie_id\n" +
            "FROM `mflix-sample`._default.movies\n" +
                "INNER JOIN (" +
                    "SELECT DISTINCT RAW schedule.movieId\n" +
                    "FROM `mflix-sample`._default.theaters\n" +
                    "UNNEST schedule\n" +
                    "WHERE schedule.hourBegin > \"18:00\") AS movie_id ON movie_id = movies._id;"
        );

        return result.rowsAs(JsonObject.class);
    }


}
