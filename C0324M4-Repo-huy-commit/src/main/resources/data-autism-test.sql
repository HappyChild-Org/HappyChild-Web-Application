-- Insert sample autism test
INSERT INTO autism_tests (test_name, description, age_range_min, age_range_max, status, created_at) 
VALUES ('Đánh giá sàng lọc tự kỷ cho trẻ em', 'Bài đánh giá sàng lọc tự kỷ dựa trên 20 câu hỏi từ các chuyên gia', 3, 12, 'ACTIVE', NOW());

-- Get the ID of the test
SET @test_id = LAST_INSERT_ID();

-- Insert questions for Communication - Language category
INSERT INTO autism_questions (question, category, weight_score) VALUES 
('Trẻ có rất ít hoặc không có giao tiếp bằng mắt?', 'Communication - Language', 2),
('Trẻ không đáp lại khi được gọi tên?', 'Communication - Language', 2),
('Trẻ không thể chỉ vào đồ vật khi được yêu cầu?', 'Communication - Language', 2),
('Trẻ không bắt chước hành động hoặc âm thanh?', 'Communication - Language', 1);

-- Insert questions for Gross motor category
INSERT INTO autism_questions (question, category, weight_score) VALUES 
('Trẻ có dáng đi bất thường hoặc đi kiễng chân?', 'Gross motor', 1),
('Trẻ thường xoay tròn hoặc lắc lư thân mình?', 'Gross motor', 2),
('Trẻ gặp khó khăn trong việc thực hiện các động tác vận động thô?', 'Gross motor', 1);

-- Insert questions for Fine motor category
INSERT INTO autism_questions (question, category, weight_score) VALUES 
('Trẻ gặp khó khăn khi sử dụng đồ chơi nhỏ hoặc bút chì?', 'Fine motor', 1),
('Trẻ không thể tự mình thực hiện các hoạt động tự chăm sóc đơn giản (như mặc quần áo)?', 'Fine motor', 1),
('Trẻ có xu hướng sắp xếp đồ vật thành hàng hoặc theo một trật tự nhất định?', 'Fine motor', 2);

-- Insert questions for Imitate and learn category
INSERT INTO autism_questions (question, category, weight_score) VALUES 
('Trẻ không bắt chước hành động của người khác?', 'Imitate and learn', 2),
('Trẻ không tham gia vào trò chơi giả vờ?', 'Imitate and learn', 2),
('Trẻ học những kỹ năng mới một cách chậm chạp hoặc khó khăn?', 'Imitate and learn', 1);

-- Insert questions for Personal - Social category
INSERT INTO autism_questions (question, category, weight_score) VALUES 
('Trẻ không quan tâm đến bạn bè cùng trang lứa?', 'Personal - Social', 2),
('Trẻ không phản ứng với cảm xúc của người khác?', 'Personal - Social', 2),
('Trẻ thích chơi một mình hơn là với người khác?', 'Personal - Social', 1),
('Trẻ không chủ động chia sẻ niềm vui hoặc thành tựu với người khác?', 'Personal - Social', 1);

-- Insert questions for Other extraordinary signs category
INSERT INTO autism_questions (question, category, weight_score) VALUES 
('Trẻ phản ứng mạnh với tiếng ồn hoặc ánh sáng?', 'Other extraordinary signs', 1),
('Trẻ bị ám ảnh với một chủ đề hoặc hoạt động cụ thể?', 'Other extraordinary signs', 2),
('Trẻ thực hiện hành vi lặp đi lặp lại (như vẫy tay hoặc lắc người)?', 'Other extraordinary signs', 2); 