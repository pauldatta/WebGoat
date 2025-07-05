# intelligent_chat_analyzer_mcp.py

import json
import os
import re
import logging
import pandas as pd
from fastmcp.server import FastMCP
from typing import Optional, List, Dict, Any

# --- logging setup ---
# Using logging is better practice for servers than print()
logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s"
)


class IntelligentChatAnalyzer:
    """
    A dynamic and comprehensive service to load, analyze, and validate insights
    from chat history files. It is equipped with both report-specific and generic
    data analysis tools to serve as an intelligent backend for a language model.
    """

    def __init__(self):
        """Initializes the analyzer, linking it to the FastMCP application instance."""
        self.loaded_files: List[str] = []
        self.chat_df: pd.DataFrame = self._create_empty_df()
        logging.info("IntelligentChatAnalyzer initialized.")
        logging.info("Use the 'load_chat_files' tool to begin an analysis session.")

    def _create_empty_df(self) -> pd.DataFrame:
        """Creates an empty DataFrame with the expected column structure."""
        columns = [
            "turn_number",
            "role",
            "content_type",
            "text_content",
            "tool_name",
            "tool_args",
            "tool_response_output",
            "tool_response_error",
        ]
        return pd.DataFrame(columns=columns)

    def _parse_file_to_df(self, file_path: str) -> Optional[pd.DataFrame]:
        # (This internal parsing logic remains the same)
        try:
            with open(file_path, "r") as f:
                chat_data = json.load(f)
        except (json.JSONDecodeError, FileNotFoundError):
            return None
        parsed_turns = []
        for turn in chat_data:
            details = {
                "role": turn.get("role"),
                "content_type": [],
                "text_content": "",
                "tool_name": None,
                "tool_args": None,
                "tool_response_output": None,
                "tool_response_error": None,
            }
            for part in turn.get("parts", []):
                if "text" in part:
                    details["content_type"].append("text")
                    details["text_content"] += part["text"] + "\n"
                elif "functionCall" in part:
                    details["content_type"].append("tool_call")
                    call = part["functionCall"]
                    details["tool_name"] = call.get("name")
                    details["tool_args"] = json.dumps(call.get("args", {}))
                elif "functionResponse" in part:
                    details["content_type"].append("tool_response")
                    resp_part = part["functionResponse"]
                    details["tool_name"] = resp_part.get("name")
                    resp_data = resp_part.get("response", {})
                    if "output" in resp_data:
                        details["tool_response_output"] = str(resp_data["output"])
                    if "error" in resp_data:
                        details["tool_response_error"] = str(resp_data["error"])
            details["content_type"] = ", ".join(
                sorted(list(set(details["content_type"])))
            )
            details["text_content"] = details["text_content"].strip()
            parsed_turns.append(details)
        return pd.DataFrame(parsed_turns)

    # --- State Management Tools ---

    def load_chat_files(self, file_paths: List[str]) -> Dict[str, Any]:
        """
        Loads and appends chat history from one or more JSON files into memory.

        Args:
            file_paths: A list of absolute or relative paths to the JSON chat files.

        Returns:
            A dictionary containing the status of the operation, including a list
            of successfully loaded files, the total number of turns now in memory,
            and a list of any errors encountered.
        """
        newly_loaded_dfs, successfully_loaded, errors = [], [], []
        for path in file_paths:
            if not os.path.exists(path):
                errors.append(f"File not found: {path}")
                continue
            df = self._parse_file_to_df(path)
            if df is not None:
                newly_loaded_dfs.append(df)
                successfully_loaded.append(path)
            else:
                errors.append(f"Failed to parse (invalid JSON?): {path}")
        if newly_loaded_dfs:
            self.chat_df = pd.concat(
                [self.chat_df] + newly_loaded_dfs, ignore_index=True
            )
            self.chat_df["turn_number"] = self.chat_df.index
            self.loaded_files.extend(successfully_loaded)
            logging.info(
                f"Successfully loaded {len(successfully_loaded)} files. Total turns in memory: {len(self.chat_df)}."
            )
        if errors:
            logging.warning(f"Encountered errors while loading files: {errors}")
        return {
            "status": "completed",
            "files_loaded": successfully_loaded,
            "total_turns_in_memory": len(self.chat_df),
            "errors": errors,
        }

    def reset_history(self) -> Dict[str, str]:
        """Clears the currently loaded chat history from memory."""
        self.chat_df = self._create_empty_df()
        self.loaded_files = []
        logging.info("Chat history has been reset.")
        return {"status": "success", "message": "Chat history has been cleared."}

    # --- Generic Data Analysis Tools ---

    def search_turns(
        self,
        query: str,
        search_in: Optional[List[str]] = None,
        use_regex: bool = False,
        case_sensitive: bool = False,
        limit: int = 10,
    ) -> List[Dict[str, Any]]:
        """
        Performs a powerful search across chat history fields for text or a regex pattern.

        Args:
            query: The text or regex pattern to search for.
            search_in: A list of columns to search within. Defaults to a broad set of text
                       fields (e.g., 'text_content', 'tool_args').
            use_regex: If True, treats the query as a regular expression. Defaults to False.
            case_sensitive: If True, the search is case-sensitive. Defaults to False.
            limit: The maximum number of matching turns to return. Defaults to 10.

        Returns:
            A list of dictionaries, where each dictionary represents a matching turn
            and includes the 'turn_number', 'role', and a 'context' snippet.
        """
        if self.chat_df.empty:
            return [{"error": "No data loaded."}]
        if search_in is None:
            search_in = [
                "text_content",
                "tool_args",
                "tool_response_output",
                "tool_response_error",
            ]
        results, search_series = [], self.chat_df[search_in].fillna("").astype(str).agg(
            " ".join, axis=1
        )
        flags = 0 if case_sensitive else re.IGNORECASE
        matches = search_series.str.contains(
            query, regex=use_regex, flags=flags, na=False
        )
        matching_df = self.chat_df[matches].head(limit)
        for _, row in matching_df.iterrows():
            match_context = ""
            for field in search_in:
                if row[field] and re.search(query, str(row[field]), flags=flags):
                    match_context = f"Match in '{field}': ...{str(row[field])[:150]}..."
                    break
            results.append(
                {
                    "turn_number": row["turn_number"],
                    "role": row["role"],
                    "context": match_context,
                }
            )
        return results

    def get_turn_context(
        self, turn_number: int, window_size: int = 3
    ) -> List[Dict[str, Any]]:
        """
        Retrieves a sequence of turns centered around a specific turn number for context.

        Args:
            turn_number: The central turn number to retrieve context for.
            window_size: The number of turns to retrieve before and after the central turn.
                         Defaults to 3.

        Returns:
            A list of dictionaries, each representing a turn in the context window.
            Includes 'turn_number', 'role', 'text_content', and 'tool_name'.
        """
        if self.chat_df.empty:
            return [{"error": "No data loaded."}]
        start_index = max(0, turn_number - window_size)
        end_index = min(len(self.chat_df), turn_number + window_size + 1)
        context_df = self.chat_df.iloc[start_index:end_index]
        return context_df[["turn_number", "role", "text_content", "tool_name"]].to_dict(
            "records"
        )

    def get_grouped_counts(
        self, group_by_column: str, value_column: str, limit: int = 10
    ) -> Dict[str, Any]:
        """
        Provides grouped counts of values, similar to a 'GROUP BY' operation in SQL.

        Args:
            group_by_column: The column to group the results by. Valid options are:
                             'role', 'content_type', 'tool_name'.
            value_column: The column whose values will be counted within each group.
                          Valid options are: 'role', 'content_type', 'tool_name'.
            limit: The maximum number of unique values to return per group. Defaults to 10.

        Returns:
            A nested dictionary with the counts. For example:
            { 'group1': { 'value1': count, 'value2': count }, ... }
        """
        if self.chat_df.empty:
            return {"error": "No data loaded."}
        valid_cols = ["role", "content_type", "tool_name"]
        if group_by_column not in valid_cols or value_column not in valid_cols:
            return {"error": f"Invalid column name(s). Choose from: {valid_cols}"}
        grouped_data = self.chat_df.groupby(group_by_column)[
            value_column
        ].value_counts()
        result = {}
        for (group, value), count in grouped_data.items():
            if group not in result:
                result[group] = {}
            if len(result[group]) < limit:
                result[group][value] = int(count)
        return result

    # --- Report-Specific Analysis Tools ---

    def get_interaction_statistics(self) -> Dict[str, Any]:
        """
        Provides detailed statistics about the interaction.

        This includes overall turn counts, a breakdown of turns by role, analysis of
        user conversational turns vs. automatic responses, and detailed statistics
        on agent tool usage (total calls and frequency per tool).

        Returns:
            A nested dictionary containing 'overall_stats', 'user_interaction_stats',
            and 'agent_tool_stats'.
        """
        if self.chat_df.empty:
            return {"error": "No data loaded."}
        turns_by_role = self.chat_df["role"].value_counts().to_dict()
        user_turns_df = self.chat_df[self.chat_df["role"] == "user"]
        conversational_turns = user_turns_df[
            user_turns_df["text_content"].str.strip().fillna("").astype(bool)
        ]
        user_stats = {
            "total_user_turns": turns_by_role.get("user", 0),
            "conversational_turns_count": len(conversational_turns),
            "automatic_tool_response_turns_count": turns_by_role.get("user", 0)
            - len(conversational_turns),
        }
        agent_tool_calls = self.chat_df[
            (self.chat_df["role"] == "model")
            & (self.chat_df["content_type"].str.contains("tool_call"))
        ]
        agent_tool_stats = {
            "total_calls": int(len(agent_tool_calls)),
            "usage_frequency": agent_tool_calls["tool_name"].value_counts().to_dict(),
        }
        return {
            "overall_stats": {
                "total_turns": len(self.chat_df),
                "turns_by_role": turns_by_role,
            },
            "user_interaction_stats": user_stats,
            "agent_tool_stats": agent_tool_stats,
        }

    def get_user_instruction_analysis(
        self,
        corrective_keys: Optional[List[str]] = None,
        instructive_keys: Optional[List[str]] = None,
    ) -> Dict[str, Any]:
        """
        Analyzes and classifies conversational user turns based on keywords.

        Turns are categorized as 'neutral', 'instructive', or 'corrective'.
        Default keywords are provided for classification but can be overridden for
        more flexible analysis.

        Args:
            corrective_keys: A list of keywords to identify corrective turns.
                             Defaults to a predefined list (e.g., 'wrong', 'error').
            instructive_keys: A list of keywords to identify instructive turns.
                              Defaults to a predefined list (e.g., 'fix', 'next').

        Returns:
            A dictionary containing the counts for each category and a list of the
            classified turns with their text content.
        """
        if self.chat_df.empty:
            return {"error": "No data loaded."}

        # --- Keywords are now parameters with sensible defaults ---
        if corrective_keys is None:
            corrective_keys = [
                "wrong",
                "error",
                "mistake",
                "revert",
                "don't",
                "not right",
                "failed",
                "should not",
            ]
        if instructive_keys is None:
            instructive_keys = [
                "fix",
                "now do",
                "next",
                "try",
                "focus on",
                "i want you to",
                "can you",
                "run",
            ]

        user_turns = self.chat_df[self.chat_df["role"] == "user"]
        conversational_turns = user_turns[
            user_turns["text_content"].str.strip().fillna("").astype(bool)
        ].copy()

        def classify_turn(text: str, corr_keys: List[str], inst_keys: List[str]) -> str:
            text = text.lower()
            if any(k in text for k in corr_keys):
                return "corrective"
            if any(k in text for k in inst_keys):
                return "instructive"
            return "neutral"

        conversational_turns["category"] = conversational_turns["text_content"].apply(
            lambda text: classify_turn(text, corrective_keys, instructive_keys)
        )
        return {
            "category_counts": conversational_turns["category"]
            .value_counts()
            .to_dict(),
            "classified_turns": conversational_turns[
                ["turn_number", "category", "text_content"]
            ].to_dict("records"),
        }


server = FastMCP(
    name="IntelligentChatAnalyzer",
    instructions="A dynamic and comprehensive service to load, analyze, and validate insights from chat history files.",
)
analyzer = IntelligentChatAnalyzer()

server.tool(analyzer.load_chat_files)
server.tool(analyzer.reset_history)
server.tool(analyzer.search_turns)
server.tool(analyzer.get_turn_context)
server.tool(analyzer.get_grouped_counts)
server.tool(analyzer.get_interaction_statistics)
server.tool(analyzer.get_user_instruction_analysis)


# --- Main execution block to run the MCP service ---
if __name__ == "__main__":
    server.run(transport="http", port=8000)
