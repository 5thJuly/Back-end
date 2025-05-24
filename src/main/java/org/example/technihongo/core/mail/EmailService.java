package org.example.technihongo.core.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.example.technihongo.entities.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendSimpleEmail(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom("<YOUR_EMAIL>");
        mailSender.send(message);
    }

    public void sendVerificationEmail(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        String link = "<your-fe-deployment-link>" + token;
        String subject = "Xác nhận tài khoản email";
        String body = "Vui lòng nhấn vào link để xác nhận tài khoản của bạn: " + link;

        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom("<YOUR_EMAIL>");
        mailSender.send(message);
    }

    public void sendReminderEmail(Student student) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(student.getUser().getEmail());
            helper.setSubject("Nhắc nhở học tập hàng ngày");
            helper.setText(
                    String.format(
                            "Chào %s,<br><br>" +
                                    "Đã đến giờ học của bạn! Mục tiêu hôm nay là %d phút.<br>" +
                                    "Chúc bạn học tập hiệu quả!<br><br>" +
                                    "Trân trọng,<br>TechNihongo Team",
                            student.getUser().getUserName(),
                            student.getDailyGoal()
                    ),
                    true
            );

            mailSender.send(message);
        } catch (MessagingException ignored) {
        }
    }

    public void sendViolationEmail(Student student, String flashcardSetTitle, String actionTaken, int violationCount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String studentEmail = student.getUser().getEmail();
            String studentName = student.getUser().getUserName();

            String subject;
            String body;

            if (violationCount == 1) {
                subject = "Bộ Flashcard của bạn cần được chỉnh sửa một chút! 😊";
                body = String.format(
                        "Chào %s,<br><br>" +
                                "Cảm ơn bạn đã đóng góp nội dung cho TechNihongo! 🌟 Tuy nhiên, chúng tôi nhận thấy bộ Flashcard <b>%s</b> của bạn có một số nội dung chưa phù hợp với quy tắc cộng đồng của chúng tôi:<br>" +
                                "<b>Lý do</b>: %s.<br><br>" +
                                "Bộ Flashcard của bạn đã bị ẩn khỏi chế độ công khai. Bạn có thể chỉnh sửa bộ Flashcard này trong vòng <b>24 giờ</b> để đảm bảo nó tuân thủ quy tắc. Sau khi chỉnh sửa, bạn có thể yêu cầu đánh giá lại để đưa nội dung trở lại chế độ công khai.<br><br>" +
                                "Nếu cần hỗ trợ, bạn có thể liên hệ với đội ngũ Admin qua <a href='mailto:technihongo.work@gmail.com'>technihongo.work@gmail.com</a>.<br>" +
                                "Cảm ơn bạn vì đã cùng xây dựng một cộng đồng học tập vui vẻ và chất lượng!<br><br>" +
                                "Trân trọng,<br>TechNihongo Team",
                        studentName, flashcardSetTitle, actionTaken
                );
            } else {
                subject = "Bộ Flashcard của bạn đã bị xóa";
                body = String.format(
                        "Chào %s,<br><br>" +
                                "Cảm ơn bạn vì những đóng góp cho TechNihongo! Tuy nhiên, chúng tôi rất tiếc phải thông báo bộ Flashcard <b>%s</b> của bạn đã vi phạm quy tắc cộng đồng lần thứ %d:<br>" +
                                "<b>Lý do</b>: %s.<br><br>" +
                                "Theo quy định của chúng tôi, bộ Flashcard này đã bị xóa do vi phạm từ lần thứ hai trở lên. Bạn vẫn có thể tạo các bộ Flashcard mới, nhưng vui lòng đảm bảo chúng tuân thủ quy tắc cộng đồng của chúng tôi.<br><br>" +
                                "Để hiểu rõ hơn về quy tắc cộng đồng, hoặc nếu bạn có bất kỳ thắc mắc nào, vui lòng liên hệ đội ngũ Admin qua <a href='mailto:technihongo.work@gmail.com'>technihongo.work@gmail.com</a>.<br>" +
                                "Chúng tôi mong rằng bạn sẽ tiếp tục đóng góp những nội dung chất lượng cho cộng đồng TechNihongo!<br><br>" +
                                "Trân trọng,<br>TechNihongo Team",
                        studentName, flashcardSetTitle, violationCount, actionTaken
                );
            }

            helper.setTo(studentEmail);
            helper.setSubject(subject);
            helper.setText(body, true);
            helper.setFrom("<YOUR_EMAIL>");
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send violation email to " + student.getUser().getEmail() + ": " + e.getMessage());
        }
    }
}