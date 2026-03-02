-- One-time cleanup for legacy Manus NEXT_STEP_PROMPT records.
-- Usage:
-- 1) Run the SELECT to verify target rows.
-- 2) Run the DELETE once after confirmation.

SELECT id, conversation_id, message_type, created_at, message_text
FROM ai_chat_message
WHERE conversation_id LIKE 'manus:%'
  AND message_type = 'USER'
  AND message_text LIKE 'Based on user needs, proactively select the most appropriate tool or combination of tools.%'
  AND message_text LIKE '%For complex tasks, you can break down the problem and use different tools step by step to solve it.%'
  AND message_text LIKE '%After using each tool, clearly explain the execution results and suggest the next steps.%'
  AND message_text LIKE '%If you want to stop the interaction at any point, use the `terminate` tool/function call.%';

DELETE FROM ai_chat_message
WHERE conversation_id LIKE 'manus:%'
  AND message_type = 'USER'
  AND message_text LIKE 'Based on user needs, proactively select the most appropriate tool or combination of tools.%'
  AND message_text LIKE '%For complex tasks, you can break down the problem and use different tools step by step to solve it.%'
  AND message_text LIKE '%After using each tool, clearly explain the execution results and suggest the next steps.%'
  AND message_text LIKE '%If you want to stop the interaction at any point, use the `terminate` tool/function call.%';
