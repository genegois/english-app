# So, How to use this??
1. Download the code (or whatever method u know to test this code).
2. Tambahin profiles.clj di root file (di dalem english-app) dulu
3. Jalanin backend lewat dev/dev.clj. Reload semua file, abis itu jalanin (start)
4. Jalanin frontend lewat terminal npx, terus:
   - npm install shadow-cljs --save-dev
   - npm install react react-dom
   - npx shadow-cljs watch app
   - (after the compile success) go to localhost:8020 in browser

## Next to-do
[mini] 
- fixing some-things:
  
  - User bisa brute-force [generate]
  
  - Per-frontend-an: tackle jawaban kosong, undo answer, and randomized answers and choiches
  
  - User bisa ngakalin assesment, dapet kunjaw abis itu tes ulang (idk why u do this, but anyway)

[big]
- add speech and listening practice 
- add for smartphone version

## License

Copyright © 2024 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.







