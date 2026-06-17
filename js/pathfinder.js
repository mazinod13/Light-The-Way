// ─────────────────────────────────────────────────
//  pathfinder.js — BFS shortest-path through maze
//
//  Usage:
//    const path = Pathfinder.bfs(maze, rows, cols, sr, sc, er, ec);
//    // returns [{r,c}, ...] or [] if no path / same cell
// ─────────────────────────────────────────────────

const Pathfinder = (() => {
  const DIRS = [[-1,0],[1,0],[0,-1],[0,1]];

  function bfs(maze, rows, cols, sr, sc, er, ec) {
    if (sr===er && sc===ec) return [];
    const visited = Array.from({length:rows}, ()=>new Array(cols).fill(false));
    const prev    = Array.from({length:rows}, ()=>new Array(cols).fill(null));
    const queue   = [[sr,sc]];
    visited[sr][sc] = true;
    while (queue.length) {
      const [r,c] = queue.shift();
      for (const [dr,dc] of DIRS) {
        const nr=r+dr, nc=c+dc;
        if (nr<0||nr>=rows||nc<0||nc>=cols) continue;
        if (maze[nr][nc]===1) continue;
        if (visited[nr][nc]) continue;
        visited[nr][nc]=true;
        prev[nr][nc]=[r,c];
        if (nr===er && nc===ec) {
          const path=[];
          let cur=[nr,nc];
          while(cur[0]!==sr||cur[1]!==sc){ path.push({r:cur[0],c:cur[1]}); cur=prev[cur[0]][cur[1]]; }
          return path.reverse();
        }
        queue.push([nr,nc]);
      }
    }
    return [];
  }

  return { bfs };
})();
