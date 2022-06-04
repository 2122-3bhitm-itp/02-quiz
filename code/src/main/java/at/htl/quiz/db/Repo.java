package at.htl.quiz.db;

import at.htl.quiz.model.Answer;
import at.htl.quiz.model.Question;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Repo {
    Connection conn;

    public Repo() throws SQLException {
        this.conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/db", "app", "app");
    }

    public void close() throws SQLException {
        this.conn.close();
    }

    public List<Question> getQuestions(int amount) throws SQLException {
        var questions = getEmptyRandomQuestion(amount);

        PreparedStatement preparedStatement = this.conn.prepareStatement("select * from answer inner join question q using (questionid) where q.question = ?");

        questions.forEach(question -> {
            try {
                preparedStatement.setString(1, question.getText());
                preparedStatement.execute();
                ResultSet rs = preparedStatement.getResultSet();

                List<Answer> answers = new ArrayList<>();

                while(rs.next()) {
                    String text = rs.getString("answer");
                    boolean isCorrect = rs.getBoolean("iscorrect");
                    int worth = rs.getInt("worth");
                    answers.add(new Answer(text, isCorrect, worth, question));
                }
                rs.close();
                question.setAnswers(answers);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        preparedStatement.close();
        return questions;
    }

    /**
     * Gets a List of Questions that have no answers connected to them. The int value defines the length of the List.
     * @param amount
     * @return List of Questions with no answers
     * @throws SQLException if something went wrong
     */
    private List<Question> getEmptyRandomQuestion(int amount) throws SQLException {
        List<Question> questions = new ArrayList<>();

        PreparedStatement pstmt = this.conn.prepareStatement("select * from question order by random() limit ?");
        pstmt.setInt(1, amount);
        pstmt.execute();
        ResultSet rs = pstmt.getResultSet();

        while(rs.next()) {
            String text = rs.getString("question");
            int difficulty = rs.getInt("difficulty");
            questions.add(new Question(text, difficulty));
        }

        rs.close();
        pstmt.close();
        return questions;
    }
}
