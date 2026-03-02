# AI Use in This Project

Short description of how AI was used in this project.

## Summary

AI was used as a pair-programming assistant throughout this project.
- Used browser-based GPT v5.2 for design discussion and review.
- Used codex GPT v5.3 for code generation and review.
- Discussed the high-level design and break implementation into detailed, manageable steps.
- Larger feature development plans were decomposed into small, single-responsibility components.
- Code snippets were drafted with AI support. Then manually reviewed, refactored and improved.
- Codex agent was used to review implemented components and then manually refined.
- Components were then interconnected with AI support to resolve integration issues.
- This workflow reduced review complexity and helped avoid regressions.

## Details

GPT 5.2 browser based model and the Codex GPT 5.3 model were used in this project. Overall, the experience has been positive. For the most part, it can generate working code, depending on the complexity of the task and the limits of its training.

AI was not used to solve larger tasks in one shot. There are plenty of drawbacks in that approach. Without a clear plan, the generated code can become unnecessarily complex. If asked to redo the same task, it can produce a completely different implementation, which creates a lot of review overhead. Its knowledge of the latest library versions can also be imperfect, so it may use outdated syntax. This one-shot flow also consumes significant tokens and time.

What worked best was using AI to discuss the high-level approach first, then creating a detailed implementation plan broken into smaller components. From there, AI supported on each component and then on integration with the wider system. This led to a faster iteration cycle, lower review cognitive load, and better oversight, since manual improvements, refactoring and adjustments were still necessary in most cases. Codex AI was very useful for reviewing ongoing progress against the initial plan and helping prioritize upcoming tasks.

AI was also a great tool for learning new ideas and concepts. For this purpose, the browser-based model was preferable since it is more verbose and can describe a variety of different approaches, which facilitate learning.

My thinking is to use AI like a pair-programming buddy and guide it through the implementation.

